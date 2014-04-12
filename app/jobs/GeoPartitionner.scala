package jobs

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import models.GeoSquare
import models._

class GeoPartitionner(keywords: List[String], square: GeoSquare, row: Int, col: Int) extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*Map holding the results*/
  var results: Map[GeoSquare, Long] = Map()
  /*List of all the queries to send*/
  val queries = TweetQuery(keywords, square, row, col).subqueries
  /*List of listeners*/
  val listeners: List[ActorRef] = queries.map(x => ActorSystem().actorOf(Props(new Counter(x.area, self))))
  /*List of actors*/
  val acts: List[ActorRef] = queries.zip(listeners).map{ x =>
    ActorSystem().actorOf(Props(new TweetSearcher(x._1, x._2)))
  }

  def squareCoords(square: GeoSquare): (Int, Int) = ???
  val totalArea = row * col
  def totalTweetDensity = ???
  def clusterDistance(cluster1: Cluster, cluster2: Cluster) = ???
  def clusterThreshold(visibleSquare: GeoSquare) = ???

  /* 
   * clusterOnce searches for the pair of clusters within dist of each other
   * and creates a larger cluster from these two.
   * To calculate the clusters at some granularity, it should be iterated on the
   * clusters of the previous granularity until a fixed point is found.
   */
  def clusterOnce(startClusters: List[List[Cluster]], dist: Int) = ???

  def clusters(visibleSquare: GeoSquare): List[List[Cluster]] = ???

  def computeOpacity(tweetCounts: Map[GeoSquare, Long]) = {
    val maxTweets = tweetCounts.values.max
    tweetCounts.mapValues(0.5*_/maxTweets)
  }

  def receive = {  
    case StartGeo =>
      acts.foreach(_ ! "start") 
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
