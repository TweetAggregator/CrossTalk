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
  
  /* Just the static index page */
  def index = Action(Ok(views.html.index()))
  
}