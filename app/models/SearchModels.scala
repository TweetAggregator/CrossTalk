package models

import play.api.libs.json.JsValue
import akka.actor.ActorRef
import play.api.libs.json._
import play.api.mvc._

/**
 * Model of a geographic position.
 * - The parameters passed are ready to be used by the Streaming API.
 * - For the Search API, the parameters need to be converted into a central
 * position + radius, hence the calculus in the inner fields.
 *
 * NB : requirements are long1 < long2, lat1 < lat2
 */
case class GeoSquare(long1: Double, lat1: Double, long2: Double, lat2: Double) {
  assert(long1 < long2 && lat1 < lat2, "Requirements for GeoSquare are long1 < long2, lat1 < lat2, not respected here")

  /** @return the center of the GeoSquare */
  val center: (Double, Double) = ((long2 + long1) / 2, (lat2 + lat1) / 2)

  /** @return an approximation of the radius of the inner circle in kms */
  val radius: Double = {
    val earthRadius = 6378.137 /* in KM */
    val (dLat, dLong) = ((lat2 - lat1) * Math.PI / 180, (long2 - long1) * Math.PI / 180)
    val v1 = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLong / 2) * Math.sin(dLong / 2)
    val v2 = 2 * Math.atan2(Math.sqrt(v1), Math.sqrt(1 - v1))
    Math.sqrt((Math.pow(earthRadius * v2, 2) / 2)) * 2 / 3
  }

  def this(square: (Double, Double, Double, Double)) = this(square._1, square._2, square._3, square._4)
  
  def containsGeo(long: Double, lat: Double): Boolean = (long >= long1 && long <= long2 && lat >= lat1 && lat <= lat2)

  def intersects(that: GeoSquare): Boolean = {
    val coo1 = List((this.long1, this.lat1), (this.long2, this.lat1), (this.long1, this.lat2), (this.long2,this.lat2))
    val coo2 = List((that.long1, that.lat1), (that.long2, that.lat1), (that.long1, that.lat2), (that.long2,that.lat2))
    !(coo1.forall(c => !that.containsGeo(c._1, c._2)) && coo2.forall(c => !this.containsGeo(c._1, c._2)))
  }
    
}

object GeoSquare {
  implicit def geoSquareReads = Json.reads[GeoSquare]
}


/**
 * Model of a Tweet Query, based on some keywords filters and a localisation.
 *
 * A query can be split into subsquares based on rows and cols passed in
 * parameter.
 *
 * @param keywords		The list of keywords to use (logical ORs) for the query
 * @param area			The are on which to launch the query
 * @param rows			The number of rows to use to split the query
 * @param cols			The number of columns to use to split the query
 */
case class TweetQuery(keywordGroup: KeywordGroup, keywords: List[String], area: GeoSquare, rows: Int, cols: Int) {

  /**
   * @return String with formatted keywords for logical ORs. Spaces into the
   *  elements of the list are replaced by ANDs.
   */
  val kwsInSearchFormat: String = {
    if (keywords.isEmpty) sys.error("Cannot start a query with empty keywords")
    else keywords.tail.foldLeft(keywords.head)((a, b) => a + " OR " + b)
      .replaceAll(" ", "%20").replaceAll("\"", "%22").replaceAll("#", "%23").take(1000)
  }

  /**
   * @return a list of subqueries with the same keywords but subsquares split
   *  based on the 'rows' and 'cols' params.
   */
  def subqueries: List[TweetQuery] = (rows, cols) match {
    case (1, 1) => this :: Nil /* Since we cannot split it anymore */
    case _ if rows >= 1 && cols >= 1 =>
      val (dx, dy) = ((area.long2 - area.long1) / rows, (area.lat2 - area.lat1) / cols)
      def listOuter(r: Int = 0, x: Double = area.long1): List[TweetQuery] = r match {
        case _ if r < rows => listInner(x) ++ listOuter(r + 1, x + dx)
        case _ => Nil
      }
      def listInner(x: Double, c: Int = 0, y: Double = area.lat1): List[TweetQuery] = c match {
        case _ if c < cols => this.copy(area = GeoSquare(x, y, x + dx, y + dy), rows = 1, cols = 1) :: listInner(x, c + 1, y + dy)
        case _ => Nil
      }
      /* Actual execution */
      listOuter()
    case _ => sys.error("Not a valid pair of rows / cols for subqueries.")
  }
  
  /**
   * @return geoSquare of subqueries zipped with (row, cols)
   */
  def computeIndices: List[(List[String], GeoSquare, Int, Int)] = {
    val subs = subqueries.map(s => s.area)
    (for (i <- 0 until rows; j <- 0 until cols) yield (keywords, subs(i*cols+ j), i, j)).toList
  }
}

/**
 * Model a returned matching tweet from a query.
 * @param value		The Json (parse) body of the tweet.
 * @param query		The original query from which the tweet is issued.
 */
case class Tweet(value: JsValue, query: TweetQuery)
