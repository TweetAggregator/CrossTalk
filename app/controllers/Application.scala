package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import akka.actor.ActorSystem
import akka.actor.Props
import play.api.data._
import play.api.data.Forms._
import java.io.File

/**
 * Main controller (about / welcome page)
 */
object Application extends Controller {
  
  /* Just the static index page */
  def index = Action {

  	/* Just creating the requird folder to store the data if required */
  	val folder = new File("tweets")
  	if(!folder.exists) folder.mkdir

  	Ok(views.html.index())
  }
  
}
