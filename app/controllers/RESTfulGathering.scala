package controllers

import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws._
import play.Logger
import java.sql.Connection
import akka.actor.Props
import scala.concurrent.ExecutionContext.Implicits.global

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

  def sumTweetsIn(tweetCounts: Map[Square, Int], surrounding: GeoSquare): Int = {
    tweetCounts.filterKeys(k => surrounding.intersects(new GeoSquare(k))).values.sum
  }

  /** Compute the Venn diagram based on a GeoSquare. Return the required format for the venn.scala.html view. */
  def computeVenn(counts1: Map[Square, Int], counts2: Map[Square, Int], interCounts: Map[Square, Int], focussed: GeoSquare, key1: String, key2: String) = {
    val sums1 = sumTweetsIn(counts1, focussed)
    val sums2 = sumTweetsIn(counts2, focussed)
    val interSums = sumTweetsIn(interCounts, focussed)


    val sets = List((0, key1, sums1), (1, key2, sums2))
    val inters = List(((0, 1), interSums))
    val nbSet = sets.size

    (nbSet, sets, inters)
  }

  def getId(request: Request[_])(implicit conn: Connection) = request.session.get("id").map(_.toLong).getOrElse(store.getNextId)


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

  def start() = Action(parse.tolerantJson) { implicit request =>
    DB.withConnection { implicit conn =>
      val coordinates = (request.body \ "coordinates").validate[List[GeoSquare]].get
      val keys1 = (request.body \ "keys1").validate[List[String]].get
      val keys2 = (request.body \ "keys2").validate[List[String]].get
      val id = getId(request)
      val coordsWithSize = coordinates.map { c =>
        val rows = granularity(c.lat1, c.lat2)
        val cols = granularity(c.long2, c.long1)
        (c, rows, cols)
      }
      if (store.containsId(id)) {
        Logger.error("Gathering: Start BadRequest")
        BadRequest
      }
      else {
        store.addSession(id, coordsWithSize, (keys1, keys2), true)
        val manager = toRef(Props(new TweetManager(id, store)))
        manager ! StartQueriesFromDB
        //Ok.withSession("id" -> id.toString)
        Ok(Json.toJson(Map("id" -> id)))
      }
    }
  }

  // This should disappear eventually.
  // For now it just reformats the keywords and calls the normal start
  def GETstart() = Action.async { implicit request =>
    val formData = request.body.asFormUrlEncoded.get
    val coordinates = Json.parse(formData("coordinates").head).as[Array[JsValue]]
    val keys1 = formData("keys1").head.split(" ")
    val keys2 = formData("keys2").head.split(" ")
    val keywords = (keys1, keys2)
    val json = Json.toJson(Map(
      "coordinates" -> coordinates,
      "keys1" -> keys1.map(Json.toJson(_)),
      "keys2" -> keys2.map(Json.toJson(_))
    ))
    WS.url(routes.RESTfulGathering.start.absoluteURL()).post(json).map { response =>
      (response.json \ "id").asOpt[Long].map { id =>
        Redirect(routes.RESTfulGathering.display(id)).withSession("id" -> id.toString)
      } getOrElse {
        BadRequest
      }
    }
    //TODO: error handling
  }

  def pause(id: Long) = update(id, false)
  def resume(id: Long) = update(id, true)

  def update(id: Long, running: Boolean) = Action { implicit request =>
    DB.withConnection { implicit c =>
      if (store.containsId(id)) {
        store.setSessionState(id, running)
        Redirect(routes.RESTfulGathering.display(id))
      }
      else {
        BadRequest
      }
    }
  }

  def display(id: Long, fLong1: Double, fLat1: Double, fLong2: Double, fLat2: Double, viewLong: Double, viewLat: Double, zoomLevel: Double) = Action { implicit request =>
    val focussed = new GeoSquare(fLong1, fLat1, fLong2, fLat2)
    val viewCenter = (viewLong, viewLat)

    DB.withConnection { implicit c =>
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
