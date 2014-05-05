package controllers.setup

import akka.actor.ActorSystem
import akka.actor.Props
import play.api._
import play.api.mvc._
import play.api.data._
import jobs.DummyActor
import play.api.data.Forms._
import play.api.libs.json._

/**
 * Square selection on Map
 */
object Map extends Controller {

  /**
   * the initial function called when the website is first loaded
   */
  def index = Action {
    val dummyActor = ActorSystem().actorOf(Props(new DummyActor()))
    dummyActor ! "Hello World !"
    Ok(views.html.map("{lat: 46.5198, lon: 6.6335}", 12, "[]", "[]"))
  }

  /**
   * when a user selected regions and wants to get the corresponding tweets from the corresponding
   */
  def submit = Action { implicit request =>
    val reqData = request.body.asFormUrlEncoded
    println(reqData)
    
    val jsonCenter = reqData.get("viewCenter").head
    val viewCenter =  Some(Json.parse(reqData.get("viewCenter").head)).map(x => ((x \ "lat").toString.toDouble, (x \ "lon").toString.toDouble)).head
    println(viewCenter)
    
    val zoomLevel = reqData.get("zoomLevel").head.toDouble
    println(zoomLevel)
    
    val regions = Json.parse(reqData.get("coordinates").head).as[List[List[JsValue]]].map(_.map(x => ((x \ "lat").toString.toDouble, (x \ "lon").toString.toDouble)))
    println(regions)
    regions.foreach(x => println("hi there: "+x))
    val mapCorners = (-122.62740484283447, 37.83336855193153) :: (-122.21155515716552, 37.696307947895036) :: Nil
    Ok(views.html.map(
        Json.stringify(JsObject("lat" -> JsNumber(viewCenter._1) :: "lon" -> JsNumber(viewCenter._2) :: Nil)),
        zoomLevel,
        Json.stringify(JsArray(regions.map(region => JsArray(region.map(corner => JsObject("lat" -> JsNumber(corner._1) :: "lon" -> JsNumber(corner._2) :: Nil)))))),
        Json.stringify(JsArray(Nil))
    ))
  }
}