package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

/**
 * Everything related to the display of the results
 */
object Results extends Controller {

  def computeVenn = Action {
    
    val sets: List[(Int, String, Int)] = List((0, "A", 199), (1, "B", 15), (2, "C", 100))
    val nbSet = sets.size
    val inters: List[((Int, Int), Int)] = List(((0, 1), 2), ((1,2), 10), ((0,2), 70))
    
    Ok(views.html.venn(nbSet, sets, inters))
  }
}