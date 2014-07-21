package controllers

import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import play.api.cache.Cache
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

  def getId(request: Request[_])(implicit conn: Connection) = request.session.get("id").map(_.toLong).getOrElse(store.getNextId)

  def refreshVenn = Action { implicit request =>
    Ok
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
        Ok //TODO: consider returning bad request or something
      }
      else {
        store.addSession(id, coordinates, keywords, true)
        val manager = toRef(Props(new TweetManager(id, store)))
        manager ! StartQueriesFromDB
        Ok.withSession("id" -> id.toString)
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

  def display(focussed: Square, viewCenter: (Double, Double), zoomLevel: Double) = ???

}

object RESTfulGathering extends RESTfulGathering(new SQLDataStore) with Controller
