package models

import play.api.libs.json.JsValue
import java.util.Date

/**
 * Model of a geographic position. The parameters passed are ready to be used by the Streaming API.
 * For the Search API, the parameters need to be converted into a central position + radius, hence
 * the calculus in the inner fields.
 */
case class GeographicPos(lat1: Double, long1: Double, lat2: Double, long2: Double) {
  val center: (Double, Double) = ??? /* TODO */
  val radius: String = ??? /* TODO */
}

/**
 * Model of a research launcher, based on some keywords filters.
 */
case class SearchQuery(keywords: List[String], area: GeographicPos, rows: Int, cols: Int, maxAge: Date) {
  val subqueries: List[SearchQuery] = (rows, cols) match {
    case (1, 1) => this :: Nil /* Since we cannot split it anymore */
    case _ => ??? /* TODO: split the query into smaller search queries */
  }
}

/**
 * Model a returned matching tweet from a query, along with its geographic position. Since tweets
 * are JSON Value, we return a JsValue, i.e. the parsed JSON code ready to be used.
 */
case class Tweet(value: JsValue, area: GeographicPos)