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

case class Cluster(topLeft: (Int, Int), bottomRight: (Int, Int), subClusters: Set[LeafCluster]) {
  val numTweets: Int = subClusters.map(_.numTweets).sum

  def merge(that: Cluster) = ???
  
  def intersect(that: Cluster): Boolean =
    subClusters.exists(that.subClusters.contains(_: LeafCluster))

  def contains(that: Cluster): Boolean = {
    this.topLeft._1 <= that.topLeft._1 && this.topLeft._2 <= that.topLeft._2 && this.bottomRight._1 >= that.bottomRight._1 && this.bottomRight._2 >= that.bottomRight._2
  }
}

case class LeafCluster(pos: (Int, Int), numTweets: Int)
