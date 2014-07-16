package controllers

import play.api.mvc._

trait SessionState
case object Running extends SessionState
case object Paused extends SessionState

trait DataStore {
  def addSession(id: Long, coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String]), state: SessionState)
  def getSessionInfo(id: Long): (List[(Double, Double, Double, Double)], (List[String], List[String]), SessionState)
  def setSessionState(id: Long, state: SessionState): Boolean
  def getSessionTweets(id: Long): Unit//TODO
  def containsId(id: Long): Boolean
}

class RealDataStore extends DataStore {
  def addSession(id: Long, coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String]), state: SessionState) = ???
  def getSessionInfo(id: Long): (List[(Double, Double, Double, Double)], (List[String], List[String]), SessionState) = ???
  def setSessionState(id: Long, state: SessionState): Boolean = ???
  def getSessionTweets(id: Long): Unit = ???//TODO
  def containsId(id: Long): Boolean = ???
}


class RESTfulGathering(store: DataStore) { this: Controller =>
  def getId(request: Request[_]) = request.session.get("id").map(_.toLong).getOrElse(request.id)

  def start(coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String])) = Action { implicit request =>
    val id = getId(request)
    if (store.containsId(id)) {
      Ok //TODO: consider returning bad request or something
    }
    else {
      store.addSession(id, coordinates, keywords, Running)
      //TODO: spawn and start a tweet manager
      Ok.withSession("id" -> id.toString)
    }
  }

  def update(id: Long, state: SessionState) = Action { implicit request =>
    val id = getId(request)
    if (store.containsId(id)) {
      store.setSessionState(id, state)
      Ok
    }
    else {
      Ok //TODO: consider returning bad request or something
    }
  }

}

object RESTfulGathering extends RESTfulGathering(new RealDataStore) with Controller
