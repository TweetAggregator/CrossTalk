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

  type Square = (Double, Double, Double, Double)
  // For each square, three geoparts: keyword1, keyword2 and keywor1&2
  val geoParts: Map[Square, (ActorRef, ActorRef, ActorRef)] = Map()
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
    //TODO: choose grid size
    //TODO: error handling
    //  - Don't start twice

    val squaresOption = Cache.getAs[List[Square]]("squares")
    // Keywords and translations: list of tuple (initialKeyword, translations&synonyms)
    val keywordsListOption = Cache.getAs[List[(String, List[String])]]("keywords")

    (squaresOption, keywordsListOption) match {
      case (Some(squares), Some((k1, trs1)::(k2, trs2)::Nil)) =>
        try {
          val finished: MutableList[Future[_]] = MutableList()
          val keys1 = (k1::trs1).mkString(" ")
          val keys2 = (k2::trs2).mkString(" ")
          for (square <- squares) {
            val gps = (geoPart(square, List(keys1)),
                       geoPart(square, List(keys2)),
                       geoPart(square, List(keys1, keys2)))
            finished += gps._1.?(StartGeo)(1 seconds)
            finished += gps._2.?(StartGeo)(1 seconds)
            finished += gps._3.?(StartGeo)(1 seconds)
            geoParts += square -> gps
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
    //TODO: clustering
    tweetManagerRef ! Stop

    //TODO: Venn diagram
    // nbSet: Int, sets: List[(Int, String, Int)], inters: List[(Int, Int), Int)]

    //TODO: opacity
    // viewCenter: String, mapZoom: Double, regionList: String, regionDensityList: String

    Ok
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
