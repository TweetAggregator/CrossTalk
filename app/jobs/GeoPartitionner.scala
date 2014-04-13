package jobs

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import models.GeoSquare
import TweetManager._
import models._
import TweetManager._

class GeoPartitionner(keywords: List[String], square: GeoSquare, row: Int, col: Int) extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*Map holding the results*/
  var results: Map[GeoSquare, Long] = Map()
  /*List of all the queries to send*/
  val queries = TweetQuery(keywords, square, row, col).subqueries
  /*List of listeners*/
  val listeners: List[ActorRef] = queries.map(x => ActorSystem().actorOf(Props(new Counter(x.area, self))))
  
  val squareCoords: Map[GeoSquare, (Int, Int)] = queries.map(_.area).zipWithIndex.map{
    x => (x._1, (x._2 % row, x._2 % col))
  }.toMap
  //def squareCoords(square: GeoSquare): (Int, Int) = ???
  val totalArea = row * col
  def totalTweetDensity = total / totalArea

  def clusterAggregate(cluster1: Cluster, cluster2: Cluster): (Cluster, Int) = {
    val clusters = cluster1::cluster2::Nil
    val (topx, topy) =(clusters.map(_.topLeft._1).toList.min, clusters.map(_.topLeft._2).toList.min)
    val (botx, boty) = (clusters.map(_.bottomRight._1).toList.max, clusters.map(_.bottomRight._2).toList.max) 
    (Cluster((topx, topy), (botx, boty), (clusters.map(_.numTweets).sum)),(botx -topx) * (boty -topy)) 
  }
  
  /*TODO what do you intend to do with this ?*/
  def clusterThreshold(visibleSquare: GeoSquare) = ???

  /* 
   * clusterOnce searches for the pair of clusters within dist of each other
   * and creates a larger cluster from these two.
   * To calculate the clusters at some granularity, it should be iterated on the
   * clusters of the previous granularity until a fixed point is found.
   */
   //TODO I don't see what List[List is supposed to represent ? below is how I thought we could do
  def clusterOnce(startClusters: List[List[Cluster]], dist: Int) = ???


  def clusterOnce(oldClust: List[Cluster], dist: Int) = {
    val pairs = (for(s <- oldClust; e <- oldClust if(s.isOnTop(e) && s!=e)) yield (s, e))
    val aggregate = pairs.map(p => clusterAggregate(p._1, p._2)).filter(_._2 <= dist)
    //TODO New clusters should satisfy the law for clustering
    //TODO and then use the contains method along with exists to add to this list the 
    // elements that we're not clustered in the oldClust

  }
  def clusters(visibleSquare: GeoSquare): List[List[Cluster]] = ???

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
