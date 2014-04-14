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
  
  val squareCoords: Map[GeoSquare, (Int, Int)] = queries.map(_.area).zipWithIndex.map{
    x => (x._1, (x._2 % row, x._2 % col))
  }.toMap

  val totalArea = row * col
  def totalTweetDensity = total / totalArea
  
  def originalClusters: Set[Cluster] = {
    squareCoords.map(e => Cluster(Set(LeafCluster(e._2, results(e._1))))).toSet
  }

  def originalLeaf: Set[LeafCluster] = {
    squareCoords.map(e => LeafCluster(e._2, results(e._1))).toSet
  }
  
  /*@brief  Iterates to obtain 10 different hierarchy of clusters*/
  def clusterResult: List[Set[Cluster]] = {
     var res: List[Set[Cluster]] = (originalClusters)::Nil
     (1 to 10).foreach{ i => 
        val beta: Double = i.toFloat / 10.0
        res :+= clusterOnce(res.last, beta * totalArea.toDouble / CORRECTOR, (1.0 - beta * totalTweetDensity))
     }
     res
  }
  
  /*@brief  Iterates at one granularity until no more clusters are formed */
  def clusterOnce(oldC: Set[Cluster], maxArea: Double, threshold: Double): Set[Cluster] = {
   val p = (for(s <- oldC; e <- oldC) yield (aggregate(s, e))).toSet.filter(c => c.area <= maxArea && c.tweetMeter >= threshold)
   val filtered = p.filter(c => !p.exists(c2 => c2.intersect(c) && c2.tweetMeter > c.tweetMeter))
   //TODO check that, I'm not sure
   val res = filtered ++ oldC.filter(c => !filtered.exists(cc => cc.intersect(c)))
   if(res == oldC)
    oldC
   else 
    clusterOnce(res, maxArea, threshold)
  } 
 
  def toGeoSquare(cluster: Cluster): Option[GeoSquare] = {
    if(!squareCoords.values.toList.contains(cluster.topLeft) 
        || !squareCoords.values.toList.contains(cluster.bottomRight)){
      None
    }else {
      val reversed = squareCoords.map{_.swap}
      val top = reversed(cluster.topLeft)
      val bottom = reversed(cluster.bottomRight)
      Some(GeoSquare(top.long1, top.lat1, bottom.long2, bottom.lat2))
    }
  }
  /*@brief  Aggregates two cluster by computing the rectangel area that joins them
            And adds all the LeafClusters contained in this one*/
  def aggregate(c1: Cluster, c2: Cluster): Cluster = {
    val aggreg = Cluster(c1.subClusters ++ c2.subClusters)
    val toAdd = originalLeaf.filter(x => aggreg.contains(x.pos))
    aggreg.subClusters ++= toAdd
    aggreg
  }

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
