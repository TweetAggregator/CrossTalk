package controllers

import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws._
import play.Logger
import java.sql.Connection
import akka.actor.Props
import akka.actor.ScalaActorRef
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import utils.Enrichments._
import utils.AkkaImplicits._
import models._
import jobs.TweetManager


class Gathering(store: DataStore, newManager: (Long, DataStore) => ScalaActorRef) { this: Controller =>
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


  def controlDisplay = Action { implicit request =>
    request.session.get("id") match {
      case Some(id) =>
        DB.withConnection { implicit c =>
          val total = List(FirstGroup, SecondGroup, IntersectionGroup).flatMap(store.getSessionTweets(id.toLong, _)).map(_._2).sum
          Ok(views.html.gathering(total.toLong, store.getSessionInfo(id.toLong).state))
        }
      case None => Redirect(routes.Application.index)
    }
  }

  def start() = Action(parse.tolerantJson) { implicit request =>
    DB.withConnection { implicit conn =>
      val coordinates = (request.body \ "coordinates").validate[List[GeoSquare]].get
      val keys1 = (request.body \ "keys1").validate[List[String]].get
      val keys2 = (request.body \ "keys2").validate[List[String]].get
      
      val coordinatesIntersect =
        coordinates.foldLeft(false)((s, c1) => coordinates.exists(c2 => {
          s || (c1 != c2 && (c1 intersects c2))
        }))
      val keywordsIntersect = keys1.exists(keys2.contains(_))

      if (coordinatesIntersect) {
        BadRequest(views.html.errorPage("Bad Request", "The chosen areas are not disjoint."))
      }
      else if (keywordsIntersect) {
        val duplicate = keys1.find(keys2.contains(_)).get
        BadRequest(views.html.errorPage("Bad Request", "The keyword $duplicate appears in both keyword groups"))
      }
      else if (keys1.distinct.size != keys1.size) {
        BadRequest(views.html.errorPage("Bad Request", "The first group of keywords contained duplicates"))
      }
      else if (keys2.distinct.size != keys2.size) {
        BadRequest(views.html.errorPage("Bad Request", "The second group of keywords contained duplicates"))
      }
      else if (keys1.isEmpty) {
        BadRequest(views.html.errorPage("Bad Request", "The first group of keywords was empty"))
      }
      else if (keys2.isEmpty) {
        BadRequest(views.html.errorPage("Bad Request", "The second group of keywords was empty"))
      }
      else if (coordinates.isEmpty) {
        BadRequest(views.html.errorPage("Bad Request", "No areas were chosen for the query"))
      }
      else {
        val coordsWithSize = coordinates.map { c =>
          val rows = granularity(c.lat1, c.lat2)
          val cols = granularity(c.long2, c.long1)
          (c, rows, cols)
        }
        //TODO: check user is logged in, and get the id
        val id = store.addSession(1, coordsWithSize, (keys1, keys2), true)
        val manager = newManager(id, store)
        manager ! StartQueriesFromDB
        Ok(Json.toJson(Map("id" -> id))).withSession("id" -> id.toString)
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
    WS.url(routes.Gathering.start.absoluteURL()).post(json).map { response =>
    //TODO: if get a badrequest ?
      val id = (response.json \ "id").as[Long]
      Redirect(routes.Gathering.display(id)).withSession("id" -> id.toString)
    }
  }

  def IfIdExists[T](id: Long, action: Action[T]) = Action.async(action.parser) { implicit request =>
    val idInDb = DB.withConnection(implicit c => store.containsId(id))
    if (idInDb) {
      action(request)
    }
    else {
      Future.successful(BadRequest(views.html.errorPage("Bad Request", s"Query number $id was not found")))
    }
  }

  def pause(id: Long) = update(id, false)
  def resume(id: Long) = update(id, true)

  def update(id: Long, running: Boolean) = IfIdExists(id, Action { implicit request =>
    DB.withConnection { implicit c =>
      store.setSessionState(id, running)
      Redirect(routes.Gathering.sessions)
    }
  })

  def display(id: Long, fLong1: Double, fLat1: Double, fLong2: Double, fLat2: Double, viewLong: Double, viewLat: Double, zoomLevel: Double) = IfIdExists(id, Action { implicit request =>
    val focussed = new GeoSquare(fLong1, fLat1, fLong2, fLat2)
    val viewCenter = (viewLong, viewLat)

    DB.withConnection { implicit c =>
      val sessionInfo = store.getSessionInfo(id)
      val key1::_ = sessionInfo.keys1
      val key2::_ = sessionInfo.keys2
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
  })

  def sessions = Action { implicit request =>
    DB.withConnection { implicit c =>
      val sessionIds = store.getUserInfo("lewis").sessions//TODO: get the username from the session 
      val sessions = sessionIds.map(store.getSessionInfo(_))
      Ok(views.html.sessions(sessions)).withSession("userId" -> "1")
    }
  }
}

object Gathering extends Gathering(new SQLDataStore, (id, store) => toRef(Props(new TweetManager(id, store)))) with Controller
