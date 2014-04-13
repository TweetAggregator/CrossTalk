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

/*case class Cluster(topLeft: (Int, Int), bottomRight: (Int, Int), subClusters: List[Cluster]) {
  val numTweets: Int = subClusters.map(_.numTweets).sum
 }*/

case class Cluster(topLeft: (Int, Int), bottomRight: (Int, Int), val numTweets: Int) {
   def isOnTop(that: Cluster): Boolean = (this.topLeft._1 <= that.topLeft._1 && this.topLeft._2 <= that.topLeft._2)
   
   def contains(that: Cluster): Boolean = {
    this.topLeft._1 <= that.topLeft._1 && this.topLeft._2 <= that.topLeft._2 && this.bottomRight._1 >= that.bottomRight._1 && this.bottomRight._2 >= that.bottomRight._2
   }

}
