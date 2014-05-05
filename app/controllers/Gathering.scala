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

  def geoPart(square: Square, keys: List[String]): ActorRef = {
    val geoSquare = GeoSquare(square._1, square._2, square._3, square._4)
    Props(geoPartitioner(keys, geoSquare, 10, 10))
  }
  
  def start() = {
    //TODO: choose grid size
    //TODO: error handling
    //  - Don't start twice
    //  - Fail gracefully if too many keywords
    //  - Catch timeout exceptions

    val squares = Cache.getAs[List[Square]]("squares").get

    // Keywords and translations: list of tuple (initialKeyword, translations&synonyms)
    val keywordsList = Cache.getAs[List[(String, List[String])]]("keywords").get
    assert(keywordsList.size == 2)

    // Separate ANDed keywords by spaces
    val keys1::keys2::_ = for ((k, trs) <- keywordsList) yield (k::trs).mkString(" ")

    val finished: MutableList[Future[_]] = MutableList()
    for (square <- squares) {
      val gps = (geoPart(square, List(keys1)),
                 geoPart(square, List(keys2)),
                 geoPart(square, List(keys1, keys2)))
      finished += gps._1.?(StartGeo)(8 seconds)
      finished += gps._2.?(StartGeo)(8 seconds)
      finished += gps._3.?(StartGeo)(8 seconds)
      geoParts += square -> gps
    }

    finished.foreach(Await.ready(_, 10 seconds))

    tweetManagerRef ! Start
    Ok
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
