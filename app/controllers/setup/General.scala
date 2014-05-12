package controllers.setup

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache
import play.api.Play.current
import models._

/**
 * General research settings
 */
object General extends Controller {

  /** Display all the parameters selected. */
  def viewParams = Action {
    Ok(views.html.setupViews.setupSummary(Cache.getAs[List[(String, List[String])]]("keywords"), Cache.getAs[List[(Double, Double, Double, Double)]]("coordinates")))
  }
  
  /** Reset all the parameters */
  def resetParams = Action {
    Cache.remove("keywords")
    Cache.remove("coordinates")
    Redirect(routes.General.viewParams)
  }
  
}