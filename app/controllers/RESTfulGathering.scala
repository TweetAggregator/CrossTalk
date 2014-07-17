package controllers

import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

import models._


class RESTfulGathering(store: DataStore) { this: Controller =>
  def getId(request: Request[_])(implicit c: Connection) = request.session.get("id").map(_.toLong).getOrElse(store.getNextId)

  def start(coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String])) = Action { implicit request =>
    DB.withConnection { implicit c =>
      val id = getId(request)
      if (store.containsId(id)) {
        Ok //TODO: consider returning bad request or something
      }
      else {
        store.addSession(id, coordinates, keywords, true)
        //TODO: spawn and start a tweet manager
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

}

object RESTfulGathering extends RESTfulGathering(new SQLDataStore) with Controller
