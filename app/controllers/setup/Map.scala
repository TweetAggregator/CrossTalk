package controllers.setup

import akka.actor.ActorSystem
import akka.actor.Props
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import utils.Enrichments._
import play.api.Play.current

/**
 * Square selection on Map
 */
object Map extends Controller {
  def setupAll = Action { implicit request =>
    Ok(views.html.setupAll())
  }
}
