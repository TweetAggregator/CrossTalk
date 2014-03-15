package controllers

import play.api._
import play.api.mvc._
import jobs.DummyActor
import akka.actor.ActorSystem
import akka.actor.Props

object Application extends Controller {

  def index = Action {
    val dummyActor = ActorSystem().actorOf(Props(new DummyActor()))
    dummyActor ! "Hello World !"
    Ok(views.html.index("Your new application is ready."))
  }

}