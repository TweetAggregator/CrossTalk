package controllers

import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._
import play.Logger
import java.sql.Connection
import akka.actor.Props

import utils.Enrichments._
import utils.AkkaImplicits._
import models._
import jobs.TweetManager


class RESTfulGathering(store: DataStore) { this: Controller =>
  type Square = (Double, Double, Double, Double)

  /* General configurations */
  val maxGranularity = getConfInt("gathering.maxGranularity", "Gathering: no granularity found in conf.")
  val minSide = getConfDouble("gathering.minSideGeo", "Gathering: no minSide") // Minumum side in degree

  /** Compute the number of rows / cols  for a research based on geocoordinates */
  def granularity(top: Double, bottom: Double): Int = {
    require(top > bottom)
    val highest: Int = (Math.ceil(top - bottom) / minSide).toInt
    if (highest > maxGranularity) maxGranularity
    else highest
  }

  val opacityCorrector = getConfDouble("GeoPartitionner.opacityCorrector", "Geopartitionner: cannot find opacity corrector")
  val minOpacity = getConfDouble("GeoPartitionner.minOpacity", "Geopartitionner: cannot find min opacity")
  
  def computeOpacity(tweetCounts: Map[Square, Int]): Map[Square, Double] = {
    if (tweetCounts.isEmpty) Map()
    else {
      val maxTweets = tweetCounts.values.max
      tweetCounts.mapValues {
        case v if v != 0 => minOpacity + opacityCorrector * v / maxTweets
        case _ => 0.0
      }
    }
  }

  def sumTweetsIn(tweetCounts: Map[Square, Int], surrounding: Square): Int = {
    val geoSurrounding = new GeoSquare(surrounding)
    tweetCounts.filterKeys(k => geoSurrounding.intersects(new GeoSquare(k))).values.sum
  }

  /** Compute the Venn diagram based on a GeoSquare. Return the required format for the venn.scala.html view. */
  def computeVenn(counts1: Map[Square, Int], counts2: Map[Square, Int], interCounts: Map[Square, Int], focussed: Square, key1: String, key2: String) = {
    val sums1 = sumTweetsIn(counts1, focussed)
    val sums2 = sumTweetsIn(counts2, focussed)
    val interSums = sumTweetsIn(interCounts, focussed)


    val sets = List((0, key1, sums1), (1, key2, sums2))
    val inters = List(((0, 1), interSums))
    val nbSet = sets.size

    (nbSet, sets, inters)
  }

  def getId(request: Request[_])(implicit conn: Connection) = request.session.get("id").map(_.toLong).getOrElse(store.getNextId)


  def refreshVenn = Action { implicit request =>
    val map = request.body.asFormUrlEncoded.get
    val bounds = Json.parse(map("focussed").head).as[Array[JsValue]]
      .flatMap(coo => ((coo \ "lon").toString.toDouble) :: (coo \ "lat").toString.toDouble :: Nil)
      .toList
    Cache.set("focussed", (bounds(0), bounds(3), bounds(2), bounds(1)))
    val zoomLevel = map("zoomLevel").head.toDouble
    val viewCenter = Some(Json.parse(map("viewCenter").head)).map(x => ((x \ "lon").toString.toDouble, (x \ "lat").toString.toDouble)).head
    Cache.set("zoomLevel", zoomLevel)
    Cache.set("viewCenter", viewCenter)
    
    Redirect(routes.RESTfulGathering.display)
  }

  def controlDisplay = Action { implicit request =>
    request.session.get("id") match {
      case Some(id) =>
        DB.withConnection { implicit c =>
          val total = List(FirstGroup, SecondGroup, IntersectionGroup).flatMap(store.getSessionTweets(id.toLong, _)).map(_._2).sum
          Ok(views.html.gathering(total.toLong, store.getSessionInfo(id.toLong)._3))
        }
      case None => Redirect(routes.Application.index)
    }
  }

  def start() = Action { implicit request =>
    val coordinates = Cache.getAs[List[Square]]("coordinates").get.map { c =>
      val rows = granularity(c._4, c._2)
      val cols = granularity(c._3, c._1)
      (c, rows, cols)
    }
    val keywordsList = Cache.getAs[List[(String, List[String])]]("keywords").get
    val keys1 = keywordsList(0)._1::keywordsList(0)._2
    val keys2 = keywordsList(1)._1::keywordsList(1)._2
    val keywords = (keys1, keys2)

    DB.withConnection { implicit c =>
      val id = getId(request)
      if (store.containsId(id)) {
        Logger.error("Gathering: Start BadRequest")
        BadRequest
      }
      else {
        store.addSession(id, coordinates, keywords, true)
        val manager = toRef(Props(new TweetManager(id, store)))
        manager ! StartQueriesFromDB
        Redirect(routes.RESTfulGathering.controlDisplay).withSession("id" -> id.toString)
      }
    }
  }

  def update(id: Long, running: Boolean) = Action { implicit request =>
    DB.withConnection { implicit c =>
      val id = getId(request)
      if (store.containsId(id)) {
        store.setSessionState(id, running)
        Ok
      }
      else {
        Ok //TODO: consider returning bad request or something
      }
    }
  }

  def display() = Action { implicit request =>
    val focussed = Cache.getAs[Square]("focussed").getOrElse((-180.0, -90.0, 180.0, 90.0))
    val viewCenter = Cache.getAs[(Double, Double)]("viewCenter").get
    val zoomLevel = Cache.getAs[Double]("zoomLevel").get

    DB.withConnection { implicit c =>
      val id = getId(request)
      val (_, (key1::_, key2::_), _) = store.getSessionInfo(id)
      val counts1 = store.getSessionTweets(id, FirstGroup)
      val counts2 = store.getSessionTweets(id, SecondGroup)
      val interCounts = store.getSessionTweets(id, IntersectionGroup)
      val (nbSet, sets, inters) =
        computeVenn(counts1, counts2, interCounts, focussed, key1, key2)
      
      val opacities1 = computeOpacity(counts1)
      val opacities2 = computeOpacity(counts2)
      val interOpacities = computeOpacity(interCounts)

      Ok(views.html.mapresult(viewCenter, zoomLevel, opacities1.toList, opacities2.toList, interOpacities.toList)(nbSet, sets, inters))
    }
  }

}

object RESTfulGathering extends RESTfulGathering(new SQLDataStore) with Controller
