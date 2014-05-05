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
  val geoParts: Map[Square, ((String, ActorRef), (String, ActorRef), ActorRef)] = Map()
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

    val squaresOption = Cache.getAs[List[Square]]("coordinates")
    // Keywords and translations: list of tuple (initialKeyword, translations&synonyms)
    val keywordsListOption = Cache.getAs[List[(String, List[String])]]("keywords")

    (squaresOption, keywordsListOption) match {
      case (Some(squares), Some((k1, trs1)::(k2, trs2)::Nil)) =>
        try {
          val finished: MutableList[Future[_]] = MutableList()
          val keys1 = k1::trs1
          val keys2 = k2::trs2
          var keys3 = for(key1 <- keys1; key2 <- keys2) yield (s"${key1} ${key2}") // Space means AND, concantenation means OR
          // val keys1 = (k1::trs1).mkString(" ")
          // val keys2 = (k2::trs2).mkString(" ")
          for (square <- squares) {
            val gps = (geoPart(square, keys1),
                       geoPart(square, keys2),
                       // geoPart(square, List(keys1, keys2)))
                      geoPart(square, keys3))
            finished += gps._1.?(StartGeo)(1 seconds)
            finished += gps._2.?(StartGeo)(1 seconds)
            finished += gps._3.?(StartGeo)(1 seconds)
            geoParts += square -> ((k1, gps._1), (k2, gps._2), gps._3)
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

    // Venn diagram
    // nbSet: Int, sets: List[(Int, String, Int)], inters: List[(Int, Int), Int)]
    // nbSet     , sets: List[(index, keyword, size)], inters: List[((index1, index2), size)]
    var sets: List[(Int, String, Int)] = Nil
    var inters: List[((Int, Int), Int)] = Nil
    geoParts.foreach(x => getInfo(x._2, sets, inters))
    val nbSet = sets.size //TODO nbSeet = size(sets) or size(inters + sets)?
    
    Ok(views.html.venn(nbSet, sets, inters))
    
    //TODO: opacity
    // viewCenter: String, mapZoom: Double, regionList: String, regionDensityList: String

    
  }

  object uniqueIndex{
    private var x = 0
    def next = x += 1
  }
  
  def getInfo(tt: ((String, ActorRef), (String, ActorRef), ActorRef), sets: List[(Int, String, Int)], inters: List[((Int, Int), Int)]) = {
    val f1 = (tt._1._2.?(TotalTweets)(1 seconds)).mapTo[Long]
    val t1 = Await.result(f1, 1 seconds)
    val s1 = (uniqueIndex.next, tt._1._1, t1)
    s1::sets
    
    val f2 = tt._2._2.? (TotalTweets)(1 seconds).mapTo[Long]
    val t2 = Await.result(f2, 1 seconds)
    val s2 = (uniqueIndex.next, tt._2._1, t2)
    s2::sets
    
    val f3 = tt._3.? (TotalTweets)(1 seconds).mapTo[Long]
    val t3 = Await.result(f3, 1 seconds)
    ( ((s1._1, s2._1), t3) )::inters
    
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
