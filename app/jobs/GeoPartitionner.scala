package jobs

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.pattern.ask
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import TweetManager._
import models._
import scala.concurrent.Await
import utils.AkkaImplicits._
import utils.Enrichments._

/** Takes care of the counters for an area designed by square, splitted according rows / cols */
class GeoPartitionner(keywords: List[String], square: GeoSquare, rows: Int, cols: Int) extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*Map holding the results*/
  var results: Map[GeoSquare, Long] = Map()
  /*List of all the queries to send*/
  val queries = TweetQuery(keywords, square, rows, cols).subqueries

  def squareCoords: Map[GeoSquare, (Int, Int)] = queries.map(_.area).zipWithIndex.map {
    x => (x._1, (x._2 % rows, x._2 / cols))
  }.toMap

  val opacityCorrector = getConfDouble("GeoPartitionner.opacityCorrector", "Geopartitionner: cannot find opacity corrector")
  val minOpacity = getConfDouble("GeoPartitionner.minOpacity", "Geopartitionner: cannot find min opacity")
  
  def computeOpacity(tweetCounts: Map[GeoSquare, Long]): Map[GeoSquare, Double] = {
    if (tweetCounts.isEmpty) Map() /* No data for now ! */
    else {
      val maxTweets = tweetCounts.values.max
      /* If we received a small amoount of tweets, we still want to see them ! */
      tweetCounts.mapValues(v => if(v !=0) (minOpacity + opacityCorrector * v / maxTweets) else 0)
    }
  }

  def receive = {
    case StartGeo =>
      val resp = TweetManagerRef.?(AddQueries(queries.map((_, self))))
      Await.ready(resp, defaultDuration)
      sender ! Done

    case Winner => println("winner is: " + results.maxBy(_._2))

    case Tweet(value, query) =>
      total += 1
      val prev: Long = results.getOrElse(query.area, 0)
      results += (query.area -> (prev + 1))

    case TotalTweets => sender ! total

    case TweetsFromSquare(square) =>
      if (results.contains(square)) sender ! results(square) /* First, see if it matches a perfect geoSquare */
      else {
        val counts = for ((s, c) <- results if square.intersects(s)) yield c
        sender ! counts.sum
      }

    case LeafClusters =>
      val leafClusters: List[LeafCluster] = for {
        geoSquare <- results.keys.toList
      } yield {
        val pos = squareCoords(geoSquare)
        LeafCluster(pos, results(geoSquare), geoSquare)
      }
      sender ! leafClusters

    case Opacities => sender ! computeOpacity(results)
  }

}
