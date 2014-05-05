package controllers

import scala.collection.mutable.Map

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache
import play.api.Play.current
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ActorSystem
import akka.pattern.ask
import play.libs.Akka

import jobs._
import models._
import utils.AkkaImplicits._
import TweetManager._

/**
 * Gathering the tweets and processing the results
 */
trait GatheringController { this: Controller =>
  type Square = (Double, Double, Double, Double)
  // For each square, three geoparts: keyword1, keyword2 and keywor1&2
  val geoParts: Map[Square, (ActorRef, ActorRef, ActorRef)] = Map()

  def geoPart(square: Square, keys: List[String]): ActorRef = {
    val geoSquare = GeoSquare(square._1, square._2, square._3, square._4)
    Props(new GeoPartitionner(keys, geoSquare, 10, 10))
  }
  
  def start() = {
    //TODO: choose grid size
    //TODO: error handling

    val squares = Cache.getAs[List[Square]]("squares").get

    // Keywords and translations: list of tuple (initialKeyword, translations&synonyms)
    val keywordsList = Cache.getAs[List[(String, List[String])]]("fromTimoToJorisAndLewis").get
    assert(keywordsList.size == 2)

    // Separate ANDed keywords by spaces
    val keys1::keys2::_ = for ((k, trs) <- keywordsList) yield (k::trs).mkString(" ")

    for (square <- squares) {
      val gps = (geoPart(square, List(keys1)),
                 geoPart(square, List(keys2)),
                 geoPart(square, List(keys1, keys2)))
      gps._1 ! StartGeo
      gps._2 ! StartGeo
      gps._3 ! StartGeo
      geoParts += square -> gps
    }

    Thread.sleep(5000)

    TweetManagerRef ! Start
    Ok
  }

  def pause = {
    TweetManagerRef ! Pause
    Ok
  }

  def resume = {
    TweetManagerRef ! Resume
    Ok
  }

  def computeDisplayData = {
    //TODO: clustering
    TweetManagerRef ! Stop

    //TODO: Venn diagram
    // nbSet: Int, sets: List[(Int, String, Int)], inters: List[(Int, Int), Int)]

    //TODO: opacity
    // viewCenter: String, mapZoom: Double, regionList: String, regionDensityList: String

    Ok
  }

}

object Gathering extends GatheringController with Controller
