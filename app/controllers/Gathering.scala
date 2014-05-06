package controllers

import scala.collection.mutable.Map
import scala.collection.mutable.MutableList

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache
import play.api.Play.current
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import play.libs.Akka
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import jobs._
import models._
import utils.AkkaImplicits._
import TweetManager._

/**
 * Gathering the tweets and processing the results
 */
trait GatheringController { this: Controller =>
  val actors: GatheringActors
  import actors._

  implicit def defaultTimeout: Timeout = 4 seconds

  type Square = (Double, Double, Double, Double)
  // For each square, three geoparts: keyword1, keyword2 and keywor1&2
  val geoParts: Map[Square, (ActorRef, ActorRef, ActorRef)] = Map()
  var keys: Option[(String, String)] = None
  val maxGranularity = 20
  val minSide = 20.0 //TODO: choose a reasonable min side

  def granularity(top: Double, bottom: Double): Int = {
    require(top > bottom)
    val highest: Int = ((top - bottom) / minSide).toInt
    if (highest > maxGranularity) maxGranularity
    else highest
  }

  def geoPart(square: Square, keys: List[String]): ActorRef = {
    val geoSquare = GeoSquare(square._1, square._2, square._3, square._4)
    val rows = granularity(square._4, square._2)
    val cols = granularity(square._3, square._1)
    Props(geoPartitioner(keys, geoSquare, rows, cols))
  }
  
  def start() = {
    //TODO: error handling
    //  - Don't start twice

    val squaresOption = Cache.getAs[List[Square]]("coordinates")
    // Keywords and translations: list of tuple (initialKeyword, translations&synonyms)
    val keywordsListOption = Cache.getAs[List[(String, List[String])]]("keywords")

    (squaresOption, keywordsListOption, keys) match {
      case (Some(squares), Some((k1, trs1)::(k2, trs2)::Nil), None) =>
        try {
          keys = Some((k1, k2))
          val finished: MutableList[Future[_]] = MutableList()
          val keys1 = k1::trs1
          val keys2 = k2::trs2
          var keys3 = for(key1 <- keys1; key2 <- keys2) yield (s"${key1} ${key2}") // Space means AND, concantenation means OR
          for (square <- squares) {
            val gps = (geoPart(square, keys1),
                       geoPart(square, keys2),
                       // geoPart(square, List(keys1, keys2)))
                      geoPart(square, keys3))
            finished += gps._1 ? StartGeo
            finished += gps._2 ? StartGeo
            finished += gps._3 ? StartGeo
            geoParts += square -> (gps._1, gps._2, gps._3)
          }

          finished.foreach(Await.ready(_, 10 seconds))

          tweetManagerRef ! Start
          Ok
        } catch {
          case e: TimeoutException => InternalServerError
        }
      case _ => BadRequest
    }
  }

  def pause = {
    tweetManagerRef ! Pause
    Ok
  }

  def resume = {
    tweetManagerRef ! Resume
    Ok
  }

  def computeDisplayData = {
    //TODO: get focussed square from Cache
    //  - get Square from Cache
    //  - filtrer les tweets

    val focussedOption = Cache.getAs[Square]("focussed")

    (focussedOption, keys) match {
      case (Some(focussed), Some((key1, key2))) =>
        try {
          //TODO: clustering

          val geoFocussed = GeoSquare(focussed._1, focussed._2, focussed._3, focussed._4)
          
          val geos1 = geoParts.values.map(_._1)
          val geos2 = geoParts.values.map(_._2)
          val geos3 = geoParts.values.map(_._3)

          val fTweets1 =
            geos1.map(a => (a ? TweetsFromSquare(geoFocussed)).mapTo[Long])
          val fTweets2 =
            geos2.map(a => (a ? TweetsFromSquare(geoFocussed)).mapTo[Long])
          val fInterTweets =
            geos3.map(a => (a ? TweetsFromSquare(geoFocussed)).mapTo[Long])

          val counts1 = fTweets1.map(Await.result(_, 4 seconds))
          val counts2 = fTweets2.map(Await.result(_, 4 seconds))
          val interCounts = fInterTweets.map(Await.result(_, 4 seconds))

          val sets =
            List((0, key1, counts1.sum.toInt), (1, key2, counts2.sum.toInt))
          val inters = List(((0, 1), interCounts.sum.toInt))
          val nbSet = sets.size //TODO nbSeet = size(sets) or size(inters + sets)?

          Ok(views.html.venn(nbSet, sets, inters))
          //TODO: opacity
          // viewCenter: String, mapZoom: Double, regionList: String, regionDensityList: String
        } catch {
          case e: TimeoutException => InternalServerError
        }
      case _ => BadRequest
    }
  }

}

abstract class GatheringActors {
  def geoPartitioner(keywords: List[String], square: GeoSquare, row: Int, col: Int): Actor
  def tweetManagerRef: ActorRef
}

class GatheringActorsImpl extends GatheringActors {
  def geoPartitioner(keywords: List[String], square: GeoSquare, row: Int, col: Int) = new GeoPartitionner(keywords, square, row, col)
  def tweetManagerRef = TweetManagerRef
}

object Gathering extends GatheringController with Controller {
  val actors =  new GatheringActorsImpl
}
