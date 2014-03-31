package models

import play.api.libs.json.JsValue
import java.util.Date

/**
 * Model of a geographic position. The parameters passed are ready to be used by the Streaming API.
 * For the Search API, the parameters need to be converted into a central position + radius, hence
 * the calculus in the inner fields.
 */
case class GeoSquare(long1: Double, lat1: Double, long2: Double, lat2: Double) {
  val center: (Double, Double) = ((long2 + long1)/2, (lat2 + lat1)/2)
  /* From stackOverflow */ 
  val radius: Double = {
    val earthRadius = 6378.137 /* in KM */
    val (dLat, dLong) = ((lat2 - lat1) * Math.PI / 180, (long2 - long1) * Math.PI / 180)
    val v1 = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLong/2) * Math.sin(dLong/2)
    val v2 = 2 * Math.atan2(Math.sqrt(v1), Math.sqrt(1-v1))
    earthRadius * v2 * 2 / 3
  }
}

/**
 * Model of a research launcher, based on some keywords filters.
 */
case class TweetQuery(keywords: List[String], area: GeoSquare, rows: Int, cols: Int) {
  def subqueries: List[TweetQuery] = (rows, cols) match {
    case (1, 1) => this :: Nil /* Since we cannot split it anymore */
    case _ if rows > 1 && cols > 1 =>
      val (dx,dy) = ((area.long2 - area.long1)/rows, (area.lat2 - area.lat1)/cols)
      def listOuter(x: Double = area.long1): List[TweetQuery] = x match {
        case _ if x >= area.long2 => Nil
        case _ => listInner(x) ++ listOuter(x+dx)
      } 
      def listInner(x: Double, y: Double = area.lat1): List[TweetQuery] = y match {
        case _ if y >= area.lat2 => Nil
        case _ => this.copy(area = GeoSquare(x,y,x+dx,y+dy), rows = 1, cols = 1) :: listInner(x,y+dy)
      }
      /* Actual execution */
      listOuter()      
    case _ => sys.error("Not a valid pair of rows / cols for subqueries.")
  }
}

/**
 * Model a returned matching tweet from a query, along with its geographic position. Since tweets
 * are JSON Value, we return a JsValue, i.e. the parsed JSON code ready to be used.
 */
case class Tweet(value: JsValue, area: GeoSquare)