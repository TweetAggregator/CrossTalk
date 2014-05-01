package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import jobs.DummyActor
import akka.actor.ActorSystem
import akka.actor.Props
import play.api.data._
import play.api.data.Forms._

/**
 * Main controller (about / welcome page)
 */
object Application extends Controller {

  def index = Action {
    val dummyActor = ActorSystem().actorOf(Props(new DummyActor()))
    dummyActor ! "Hello World !"
    Ok(views.html.index("Your new application is ready."))
  }

  def submit = Action { implicit request =>

    val reqData = request.body.asFormUrlEncoded
    println(reqData)

    val scalaCoordinatesList = List[(Double, Double, Double, Double)]()

    Ok(views.html.index2())
  }

  def designExample = Action {
    Ok(views.html.test())
  }

}