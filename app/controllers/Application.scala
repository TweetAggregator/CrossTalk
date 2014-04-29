package controllers

import play.api._
import play.api.mvc._
import jobs.DummyActor
import akka.actor.ActorSystem
import akka.actor.Props

/**
 * Main controller (about / welcome page)
 */
object Application extends Controller {

  def index = Action {
    val dummyActor = ActorSystem().actorOf(Props(new DummyActor()))
    dummyActor ! "Hello World !"
    Ok(views.html.index("Your new application is ready."))
  }
  
  def homepageTest = Action {
	Ok(views.html.test())
  }

}