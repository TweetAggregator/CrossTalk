package models

import play.api.libs.json.JsValue
import java.util.Date
import akka.actor.ActorRef

// Messages for communication between actors
case class Report(id: GeoSquare, count: Long)
case object TotalTweets
case object Winner
case object ReportCount
case object StartGeo
case object Collect

case object Start

case object Ping

case class AddQueries(queries: List[(TweetQuery, ActorRef)])

case object Stop

case object Opacities

class Cluster(topLeft: (Int, Int), bottomRight: (Int, Int), subClusters: List[Cluster]) {
  val numTweets: Int = subClusters.map(_.numTweets).sum
}

case class LeafCluster(topLeft: (Int, Int), bottomRight: (Int, Int), override val numTweets: Int) extends Cluster(topLeft, bottomRight, Nil)
