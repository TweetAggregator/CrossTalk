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

/** Takes care of the counters for an area designed by square, splitted according rows / cols */
class GeoPartitionner(keywords: List[String], square: GeoSquare, rows: Int, cols: Int) extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*Map holding the results*/
  var results: Map[GeoSquare, Long] = Map()
  /*List of all the queries to send*/
  val queries = TweetQuery(keywords, square, rows, cols).subqueries
  /*List of listeners*/
  val listeners: List[ActorRef] = queries.map(x => ActorSystem().actorOf(Props(new Counter(x.area, self))))

  def squareCoords: Map[GeoSquare, (Int, Int)] = queries.map(_.area).zipWithIndex.map {
    x => (x._1, (x._2 % rows, x._2 / cols))
  }.toMap

  def computeOpacity(tweetCounts: Map[GeoSquare, Long]): Map[GeoSquare, Double] = {
    val maxTweets = tweetCounts.values.max
    if (maxTweets == 0) tweetCounts.mapValues(_ => 0)
    else tweetCounts.mapValues(0.5 * _ / maxTweets)
  }

  def receive = {
    case StartGeo =>
      val resp = TweetManagerRef.?(AddQueries(queries zip listeners))
      Await.ready(resp, defaultDuration)
      sender ! Done

    case Winner => println("winner is: " + results.maxBy(_._2))

    case Collect => listeners.foreach(_ ! ReportCount)

    case Report(id, count) =>
      total += count
      val prev: Long = results.getOrElse(id, 0)
      results += (id -> (prev + count))

    case TotalTweets => sender ! total

    case TweetsFromSquare(square) =>
      if (results.contains(square)) sender ! results(square) /* First, see if it matches a perfect geoSquare */
      else {
        val counts = for ((s, c) <- results if square.intersects(s)) yield c
        sender ! counts.sum
      }

    case Opacities =>
      sender ! computeOpacity(results)
  }

}
