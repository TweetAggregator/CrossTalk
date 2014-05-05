package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import akka.actor.ActorSystem
import akka.actor.Props
import play.api.data._
import play.api.data.Forms._

/**
 * Main controller (about / welcome page)
 */
object Application extends Controller {
  def index = designExample
  
  def designExample = Action {
    Ok(views.html.test())
  }

}
