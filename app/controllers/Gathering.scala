package controllers

import scala.collection.mutable.{ Map => MutableMap }
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
import play.Logger
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.reflect.ClassTag
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
  
  /* For each square, three geoparts: keyword1, keyword2 and keywor1&2 */
  val geoParts: MutableMap[Square, (ActorRef, ActorRef, ActorRef)] = MutableMap()
  var keys: Option[(String, String)] = None
  val maxGranularity = 20
  val minSide = 0.5 //TODO: choose a reasonable min side

  def granularity(top: Double, bottom: Double): Int = {
    require(top > bottom)
    val highest: Int = ((top - bottom) / minSide).toInt + 1
    if (highest > maxGranularity) maxGranularity
    else highest
  }

  def geoPart(square: Square, keys: List[String]): ActorRef = {
    val geoSquare = GeoSquare(square._1, square._2, square._3, square._4)
    val rows = granularity(square._4, square._2)
    val cols = granularity(square._3, square._1)
    Props(geoPartitioner(keys, geoSquare, rows, cols))
  }

  def squareFromGeoMap[T](geoMaps: List[Map[GeoSquare, T]]) = {
    val flattenedMap = geoMaps.foldLeft(Map[GeoSquare, T]())(_ ++ _)
    for ((k, v) <- flattenedMap) yield ((k.long1, k.lat1, k.long2, k.lat2), v)
  }

  def start() = Action {
    if (!Cache.get("isStarted").isDefined) {

      //TODO remove those cache writes
      /*Cache.set("coordinates", List((-129.4, 20.0, -79.0, 50.6)))
      Cache.set("keywords",
        List(("Obama", List[String]()),
          ("Beer", List("biere", "pression"))))
      Cache.set("focussed", (-109.4, 40.0, -79.0, 50.6))*/

      val squaresOption = Cache.getAs[List[Square]]("coordinates")
      // Keywords and translations: list of tuple (initialKeyword, translations&synonyms)
      val keywordsListOption = Cache.getAs[List[(String, List[String])]]("keywords")

      (squaresOption, keywordsListOption, keys) match {
        case (Some(squares), Some((k1, trs1) :: (k2, trs2) :: Nil), None) =>
          try {
            keys = Some((k1, k2))
            val finished: MutableList[Future[_]] = MutableList()
            val keys1 = k1 :: trs1
            val keys2 = k2 :: trs2
            var keys3 = for (key1 <- keys1; key2 <- keys2) yield (s"${key1} ${key2}") // Space means AND, concantenation means OR
            for (square <- squares) {
              val gps = (geoPart(square, keys1),
                geoPart(square, keys2),
                geoPart(square, keys3))
              finished += gps._1 ? StartGeo
              finished += gps._2 ? StartGeo
              finished += gps._3 ? StartGeo
              geoParts += square -> (gps._1, gps._2, gps._3)
            }

            finished.foreach(Await.ready(_, 10 seconds))

            tweetManagerRef ! Start
            Cache.set("isStarted", true)
            Cache.set("isRunning", true)
            Redirect(routes.Gathering.controlDisplay)
          } catch {
            case e: TimeoutException => println("Gathering_Start_InternalServerError"); InternalServerError
          }
        case t => println("Gathering_Start_BadRequest\n" + t); BadRequest
      }
    } else Redirect(routes.Application.index)
  }

  def pause = Action {
    if (Cache.get("isRunning").isDefined) {
      tweetManagerRef ! Pause
      Cache.remove("isRunning")
    }
    Redirect(routes.Gathering.controlDisplay)
  }

  def resume = Action {
    if (!Cache.get("isRunning").isDefined) {
      tweetManagerRef ! Resume
      Cache.set("isRunning", true)
    }
    Redirect(routes.Gathering.controlDisplay)
  }

  def stop = Action {
    if (Cache.get("isStarted").isDefined) {
      tweetManagerRef ! Stop
      geoParts.clear()
      keys = None
      Cache.remove("isStarted")
      Cache.remove("isRunning")
      Cache.remove("coordinates")
      Cache.remove("focussed")
      Cache.remove("keywords")
    }
    Redirect(routes.Application.index)
  }

  def controlDisplay = Action {
    val allGeos = geoParts.flatMap(v => v._2._1 :: v._2._2 :: v._2._3 :: Nil)
    allGeos foreach (_ ! Collect)
    Ok(views.html.gathering(askGeos[Long](allGeos, TotalTweets).sum))
  }

  def computeDisplayData = Action {
    //TODO: get focussed square from Cache
    //  - get Square from Cache
    //  - filtrer les tweets

    val focussedOption = Cache.getAs[Square]("focussed")

    (focussedOption, keys) match {
      case (Some(focussed), Some((key1, key2))) =>
        try {
          //TODO: clustering
          Logger.info("Gathering: Computing display data")

          val geoFocussed = GeoSquare(focussed._1, focussed._2, focussed._3, focussed._4)

          val geos1 = geoParts.values.map(_._1)
          val geos2 = geoParts.values.map(_._2)
          val geos3 = geoParts.values.map(_._3)

          Logger.info("Gathering: Finalizing Tweet counts")
          geos1 foreach (_ ! Collect)
          geos2 foreach (_ ! Collect)
          geos3 foreach (_ ! Collect)

          Logger.info("Gathering: Getting Tweet counts")
          val fInterTweets =
            geos3.map(a => (a ? TweetsFromSquare(geoFocussed)).mapTo[Long])

          val counts1 = askGeos[Long](geos1, TweetsFromSquare(geoFocussed))
          val counts2 = askGeos[Long](geos2, TweetsFromSquare(geoFocussed))
          val interCounts = askGeos[Long](geos3, TweetsFromSquare(geoFocussed))

          val sets =
            List((0, key1, counts1.sum.toInt), (1, key2, counts2.sum.toInt))
          val inters = List(((0, 1), interCounts.sum.toInt))
          val nbSet = sets.size //TODO nbSeet = size(sets) or size(inters + sets)?

          Logger.info("Gathering: Computing square opacities")
          val opac1 = squareFromGeoMap(askGeos[Map[GeoSquare, Double]](geos1, Opacities))
          val opac2 = squareFromGeoMap(askGeos[Map[GeoSquare, Double]](geos2, Opacities))
          val interOpac =
            squareFromGeoMap(askGeos[Map[GeoSquare, Float]](geos3, Opacities))

          // @(viewCenter: (Double, Double), mapZoom: Double, regionDensityList: List[((Double, Double, Double, Double), Float)], nbSet:Int, sets:List[(Int, String, Int)], inters:List[((Int, Int), Int)])
          val viewCenter = (37.0, -122.0)
          Ok(views.html.mapresult(viewCenter, 12, interOpac.toList, nbSet, sets, inters))

          // Ok(views.html.venn(nbSet, sets, inters))
        } catch {
          case e: TimeoutException =>
            Logger.info("Gathering: Timed out")
            InternalServerError
        }
      case _ => BadRequest
    }
  }

  def askGeos[T: ClassTag](geos: Iterable[ActorRef], msg: Any): List[T] = {
    val futures: List[Future[T]] = geos.map(a => (a ? msg).mapTo[T]).toList
    futures.map(Await.result(_, 10 seconds))
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
  val actors = new GatheringActorsImpl
}
