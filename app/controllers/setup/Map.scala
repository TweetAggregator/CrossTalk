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
    val startLat = getConfDouble("map.startLat", "Map: no beginning Lat in Conf.")
    val startLong = getConfDouble("map.startLong", "Map: no beginning Long in Conf.")
    val startZoom = getConfInt("map.startZoom", "Map: no beginning Zoom in Conf.")

    Ok(views.html.setupAll(s"{lat: ${startLat}, lon: ${startLong}}", startZoom))
  }
}
