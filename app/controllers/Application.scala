package controllers

import play.api._
import play.api.libs.json._
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
  def submit = Action { implicite_request =>
  	val dummyActor = ActorSystem().actorOf(Props(new DummyActor()))
    dummyActor ! "Launch button clicked"
    val scalaCoordinatesList = List[(Double,Double)]()
         val list = List( "hoge\"hoge", "moge'mo\"ge" )
     val json = Json.stringify( Json.obj( 
       "list" -> JsArray( list.map( JsString(_) ) )
     ))
   Ok(views.html.index2(json))


  }

}