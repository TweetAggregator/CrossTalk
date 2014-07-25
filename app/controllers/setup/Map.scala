package controllers.setup

import akka.actor.ActorSystem
import akka.actor.Props
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import utils.Enrichments._
import play.api.cache.Cache
import play.api.Play.current

/**
 * Square selection on Map
 */
object Map extends Controller {

  /**
   * Display the area selecter for the setup.
   */
  def selectAreas = Action {
    val startLat = getConfDouble("map.startLat", "Map: no beginning Lat in Conf.")
    val startLong = getConfDouble("map.startLong", "Map: no beginning Long in Conf.")
    val startZoom = getConfInt("map.startZoom", "Map: no beginning Zoom in Conf.")

    Ok(views.html.setupViews.map(s"{lat: ${startLat}, lon: ${startLong}}", startZoom))
  }

  /**
   * Submission of the area selected. Set the area inside the Cache and return to the parameters view.
   */
  def finalSubmission = Action { implicit request =>
    request.body.asFormUrlEncoded match {
      case Some(map) if map("coordinates").head != "" =>
        
        val coordinates = Json.parse(map("coordinates").head).as[Array[Array[JsValue]]]
          .map(_.flatMap(coo => ((coo \ "lon").toString.toDouble) :: (coo \ "lat").toString.toDouble :: Nil))
          .map(e => (e(0), e(1), e(2), e(3))).toList
          // .map(e => (e(3), e(0), e(1), e(2))).toList
        val zoomLevel = map("zoomLevel").head.toDouble
        val viewCenter = Some(Json.parse(map("viewCenter").head)).map(x => ((x \ "lon").toString.toDouble, (x \ "lat").toString.toDouble)).head
        
        Cache.set("zoomLevel", zoomLevel)
        Cache.set("viewCenter", viewCenter)
        Cache.set("coordinates", coordinates)
        
        Redirect(routes.General.viewParams)
      case _ => Redirect(routes.Map.selectAreas)
    }
  }
}
