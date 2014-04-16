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

case class Cluster(topLeft: (Int, Int), bottomRight: (Int, Int),var subClusters: Set[LeafCluster]) {
  def numTweets: Long = subClusters.map(_.numTweets).sum 
  
  def intersect(that: Cluster): Boolean =
    subClusters.exists(that.subClusters.contains(_: LeafCluster))

  def strictIntersect(that: Cluster): Boolean = this.intersect(that) && !(this.contains(that) || that.contains(this))

  def contains(that: Cluster): Boolean = 
    that.subClusters.forall(this.subClusters.contains(_))
  
  def contains(point: (Int, Int)): Boolean = {
    topLeft._1 <= point._1 && topLeft._2 <= point._2 && bottomRight._1 >= point._1 && bottomRight._2 >= point._2
  }

  def area = subClusters.size
  def tweetMeter: Double = this.numTweets.toFloat / this.area.toFloat
  def <(that: Cluster): Boolean = subClusters.size < that.subClusters.size
  def >(that: Cluster): Boolean = subClusters.size > that.subClusters.size
}
object Cluster {
  def apply(subClusters: Set[LeafCluster]): Cluster = {
    val x = subClusters.map(_.pos._1)
    val y = subClusters.map(_.pos._2)
    new Cluster((x.min, y.min), (x.max, y.max), subClusters)
  }
}
case class LeafCluster(pos: (Int, Int), numTweets: Long)

case class Pixel(pos:(Int, Int),var l: Int,var d: Double)
case class SuperPixel (k: Int,var pos:(Int, Int), var d: Double){
  def getCopy: SuperPixel = SuperPixel(k, pos, d)
}
