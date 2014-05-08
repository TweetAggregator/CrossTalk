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

    Ok(views.html.setupViews.map(s"{lat: ${startLat}, lon: ${startLong}}", startZoom, "[]", "[]"))
  }

  /**
   * Submission of the area selected. Set the area inside the Cache and return to the parameters view.
   * TODO: rename, perhaps, one submit (which do more than just submit) is renamed.
   */
  def finalSubmission = Action { implicit request =>
    request.body.asFormUrlEncoded match {
      case Some(map) if map("coordinates").head != "" =>
        
        val coordinates = Json.parse(map("coordinates").head).as[Array[Array[JsValue]]]
          .map(_.flatMap(coo => ((coo \ "lon").toString.toDouble) :: (coo \ "lat").toString.toDouble :: Nil))
          .map(e => (e(3), e(0), e(1), e(2))).toList
        val zoomLevel = map("zoomLevel").head.toDouble
        val viewCenter = Some(Json.parse(map("viewCenter").head)).map(x => ((x \ "lat").toString.toDouble, (x \ "lon").toString.toDouble)).head
        
        Cache.set("zoomLevel", zoomLevel)
        Cache.set("focussed", viewCenter)
        Cache.set("coordinates", coordinates)
        
        Redirect(routes.General.viewParams)
      case _ => Redirect(routes.Map.selectAreas)
    }
  }

  /**
   * TODO: cleanup
   */
  def index = Action {
    Ok(views.html.map("""{"lon": 6.6335, "lat": 46.5198}""", 12, "[]", "[]"))
  }

  /**
   * TODO: cleanup
   */
  def submit = Action { implicit request =>
    val reqData = request.body.asFormUrlEncoded
    println("Data " + reqData)
    //val scalaCoordinatesList = List[(Double, Double, Double, Double)]();
    val jsonCenter = reqData.get("viewCenter").head
    val viewCenter =  Some(Json.parse(reqData.get("viewCenter").head)).map(x => ((x \ "lon").toString.toDouble, (x \ "lat").toString.toDouble)).head
    val zoomLevel = reqData.get("zoomLevel").head.toDouble
    val testRegions = Json.parse(reqData.get("coordinates").head).as[List[List[JsValue]]]
    	.map(_.map(x => ((x \ "lon").toString.toDouble, (x \ "lat").toString.toDouble)))
    		.map(x => ((x(0)._1, x(0)._2, x(1)._1, x(1)._2), scala.util.Random.nextFloat))
    //TODO: il faut ces vauleurs Scala pour le passer à l'autre controlleur!!
    
    //val mapCorners = (-122.62740484283447, 37.83336855193153) :: (-122.21155515716552, 37.696307947895036) :: Nil
    Ok(views.html.mapresult(
        viewCenter,
        zoomLevel,
        testRegions,
        
        0,
        Nil,
        Nil
    ))
  }
}