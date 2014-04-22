package jobs

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import models.GeoSquare
import TweetManager._
import models._
import TweetManager._

class GeoPartitionner(keywords: List[String], square: GeoSquare, row: Int, col: Int) extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*TODO play around with this to correct the threshold*/
  val CORRECTOR: Double = 4
  /*Map holding the results*/
  var results: Map[GeoSquare, Long] = Map()
  /*List of all the queries to send*/
  val queries = TweetQuery(keywords, square, row, col).subqueries
  /*List of listeners*/
  val listeners: List[ActorRef] = queries.map(x => ActorSystem().actorOf(Props(new Counter(x.area, self))))
  
  def squareCoords: Map[GeoSquare, (Int, Int)] = queries.map(_.area).zipWithIndex.map{
    x => (x._1, (x._2 % row, x._2 / col))
  }.toMap

  def computeOpacity(tweetCounts: Map[GeoSquare, Long]) = {
    val maxTweets = tweetCounts.values.max
    tweetCounts.mapValues(0.5*_/maxTweets)
  }

  def receive = {  
    case StartGeo =>
      TweetManagerRef ! AddQueries(queries zip listeners)
    case Winner => 
      println("winner is: "+results.maxBy(_._2))  
    case Collect => 
      listeners.foreach(_ ! ReportCount)
    case Report(id, count) =>
      total += count
      results += (id -> count)
    case TotalTweets => 
      sender ! total
    case Opacities =>
      sender ! computeOpacity(results)
  }
  
}
