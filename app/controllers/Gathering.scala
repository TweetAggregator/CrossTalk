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
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import jobs._
import models._
import clustering._
import utils.AkkaImplicits._
import TweetManager._

/**
 * Gathering the tweets and processing the results. Contains the following actions:
 * 		1. Start a research
 *   	2. Pause a research
 *    	3. Stop a research
 *     	4. Gather the results for standard display (no clustering)
 *      5. Gather the results for clustering (asynchronously)
 *      6. Select the areas for the Venn Diagram
 */
trait GatheringController { this: Controller =>
  val actors: GatheringActors
  import actors._
  import utils.Enrichments._

  type Square = (Double, Double, Double, Double)

  /** For each square, three geoparts: keyword1, keyword2 and keywor1&2 */
  val geoParts: MutableMap[Square, (ActorRef, ActorRef, ActorRef)] = MutableMap()
  val geoPartSizes: MutableMap[ActorRef, (Int, Int)] = MutableMap()
  
  /* Let's have a lists of geoPart for each keywords */
  def geos1 = geoParts.values.map(_._1)
  def geos2 = geoParts.values.map(_._2)
  def geos3 = geoParts.values.map(_._3)

  /** Default keys of the research */
  var keys: Option[(String, String)] = None

  /* General configurations */
  val maxGranularity = getConfInt("gathering.maxGranularity", "Gathering: no granularity found in conf.")
  val minSide = getConfDouble("gathering.minSideGeo", "Gathering: no minSide") // Minumum side in degree

  /** Compute the number of rows / cols  for a research based on geocoordinates */
  def granularity(top: Double, bottom: Double): Int = {
    require(top > bottom)
    val highest: Int = (Math.ceil(top - bottom) / minSide).toInt
    if (highest > maxGranularity) maxGranularity
    else highest
  }

  /** From a square and a list of keywords, generate a GeoPartitioner. Compute the number of rows / cols to use */
  def geoPart(square: Square, keys: List[String]): ActorRef = {
    val geoSquare = GeoSquare(square._1, square._2, square._3, square._4)
    val rows = granularity(square._4, square._2)
    val cols = granularity(square._3, square._1)
    val geoPart: ActorRef = Props(geoPartitioner(keys, geoSquare, rows, cols))
    geoPartSizes += (geoPart -> (rows, cols))
    geoPart
  }

  def squareFromGeoMap[T](geoMaps: List[Map[GeoSquare, T]]) = {
    val flattenedMap = geoMaps.foldLeft(Map[GeoSquare, T]())(_ ++ _)
    for ((k, v) <- flattenedMap) yield ((k.long1, k.lat1, k.long2, k.lat2), v)
  }

  def start() = Action {
    if (!Cache.get("isStarted").isDefined) {

      Cache.set("focussed", (-180.0, -90.0, 180.0, 90.0))

      val squaresOption = Cache.getAs[List[Square]]("coordinates")
      val keywordsListOption = Cache.getAs[List[(String, List[String])]]("keywords")

      (squaresOption, keywordsListOption, keys) match {
        case (Some(squares), Some((k1, trs1) :: (k2, trs2) :: Nil), None) =>
          try {
            keys = Some((k1, k2))
            val keys1 = k1 :: trs1
            val keys2 = k2 :: trs2
            val keys3 = for (key1 <- keys1; key2 <- keys2) yield (s"${key1} ${key2}") // Space means AND, concantenation means OR

            var finished: List[Future[_]] = List()
            for (square <- squares) {
              val gps = (geoPart(square, keys1), geoPart(square, keys2), geoPart(square, keys3))
              finished :+= gps._1 ? StartGeo
              finished :+= gps._2 ? StartGeo
              finished :+= gps._3 ? StartGeo
              geoParts += square -> (gps._1, gps._2, gps._3)
            }
            finished.foreach(Await.ready(_, defaultDuration))

            Logger.info("Gathering: Starting TweetManager")
            tweetManagerRef ! Start
            Cache.set("isStarted", true)
            Cache.set("isRunning", true)
            Redirect(routes.Gathering.controlDisplay)
          } catch {
            case e: TimeoutException =>
              Logger.error("Gathering:_Start_InternalServerError, due to timeout.")
              InternalServerError
          }
        case _ =>
          Logger.error("Gathering:_Start_BadRequest")
          BadRequest
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

  /** Stop and delete all statistics (everything to original state) */ 
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

  /** Print the various actions doable on a research and the total number of tweet found */
  def controlDisplay = Action {
    val allGeos = geoParts.flatMap(v => v._2._1 :: v._2._2 :: v._2._3 :: Nil)
    Ok(views.html.gathering(askGeos[Long](allGeos, TotalTweets).sum))
  }
  
  /** Refresh the Venn diagram based on the new area selection */
  def refreshVenn = Action { implicit request =>
    
    request.body.asFormUrlEncoded match {
      case Some(map) if map("focussed").head != "" =>
      /* First, let's get the boundaries of the map */
        val bounds = Json.parse(map("focussed").head).as[Array[JsValue]]
          .flatMap(coo => ((coo \ "lon").toString.toDouble) :: (coo \ "lat").toString.toDouble :: Nil)
          .toList
        Cache.set("focussed", (bounds(0), bounds(3), bounds(2), bounds(1)))
        /* Now let's update the center and the zoomlevel */
        val zoomLevel = map("zoomLevel").head.toDouble
        val viewCenter = Some(Json.parse(map("viewCenter").head)).map(x => ((x \ "lon").toString.toDouble, (x \ "lat").toString.toDouble)).head
        Cache.set("zoomLevel", zoomLevel)
        Cache.set("viewCenter", viewCenter)
      case _ => Nil /* Should never happen (would be an error in the JS of tweetagg.js) */
    }
    /* Redirect to the computation of the display */
    Redirect(routes.Gathering.computeDisplayData)
  }

  /** Compute the non-clustered view */
  def computeDisplayData = Action {

    val focussedOption = Cache.getAs[Square]("focussed")
    val viewCenterOption = Cache.getAs[(Double, Double)]("viewCenter")
    val zoomLevelOption = Cache.getAs[Double]("zoomLevel")

    (viewCenterOption, zoomLevelOption, focussedOption, keys) match {
      case (Some(viewCenter), Some(zoomLevel), Some(focussed), Some((key1, key2))) =>
        try {
          Logger.info("Gathering: compute Venn.")
          val (nbSet, sets, inters) = computeVenn(GeoSquare(focussed._1, focussed._2, focussed._3, focussed._4), key1, key2)

          Logger.info("Gathering: Computing square opacities")
          val opac1 = squareFromGeoMap(askGeos[Map[GeoSquare, Double]](geos1, Opacities))
          val opac2 = squareFromGeoMap(askGeos[Map[GeoSquare, Double]](geos2, Opacities))
          val interOpac = squareFromGeoMap(askGeos[Map[GeoSquare, Float]](geos3, Opacities))

          Ok(views.html.mapresult(viewCenter, zoomLevel, opac1.toList, opac2.toList, interOpac.toList)(nbSet, sets, inters))

        } catch {
          case e: TimeoutException =>
            Logger.info("Gathering: Timed out")
            InternalServerError
        }
      case _ =>
        Logger.error("Gathering: Computing display data BadRequest")
        BadRequest
    }
  }

  /** Compute the clustered view */
  def computeDisplayClustering = Action {

    val viewCenterOption = Cache.getAs[(Double, Double)]("viewCenter")
    val zoomLevelOption = Cache.getAs[Double]("zoomLevel")

    (viewCenterOption, zoomLevelOption, keys) match {
      case (Some(viewCenter), Some(zoomLevel), Some((key1, key2))) =>
        try {
          
          /* Let's parallelize the clustering using Futures */
          val futuresClust = (geos1 :: geos2 :: geos3 :: Nil).map(g => scala.concurrent.Future{computeClusters(g)})
          val clusters = futuresClust.map(f => Await.result(f, defaultDuration))
          
          Ok(views.html.mapClustering((key1, key2), viewCenter, zoomLevel, (clusters(0), clusters(1), clusters(2))))
        } catch {
          case e: TimeoutException =>
            Logger.info("Gathering: Timed out")
            InternalServerError
        }
      case _ =>
        Logger.error("Gathering: Computing clusters BadRequest")
        BadRequest
    }
  }

  /** From a list of geopartitionners, compute the clustering */
  def computeClusters(geos: Iterable[ActorRef]): List[Set[Cluster]] = {
    val leafClusters = askGeos[List[LeafCluster]](geos, LeafClusters)
    val clusters: Iterable[List[Set[Cluster]]] =
      for ((geoPart, leaves) <- (geos3 zip leafClusters)) yield {
        val (rows, cols) = geoPartSizes(geoPart)
        val clust = new ClustHC(leaves, rows, cols)
        clust.compute
      }
    (for (i <- 0 until clusters.head.size) yield {
      clusters.flatMap(_(i)).toSet
    }).toList

  }

  /** Compute the Venn diagram based on a GeoSquare. Return the required format for the venn.scala.html view. */
  def computeVenn(geoFocussed: GeoSquare, key1: String, key2: String) = {
    val counts1 = askGeos[Long](geos1, TweetsFromSquare(geoFocussed))
    val counts2 = askGeos[Long](geos2, TweetsFromSquare(geoFocussed))
    val interCounts = askGeos[Long](geos3, TweetsFromSquare(geoFocussed))

    val sets = List((0, key1, counts1.sum.toInt), (1, key2, counts2.sum.toInt))
    val inters = List(((0, 1), interCounts.sum.toInt))
    val nbSet = sets.size

    (nbSet, sets, inters)
  }

  /** Send a message to all the GeoPartitioners and return the answers as a list */
  def askGeos[T: ClassTag](geos: Iterable[ActorRef], msg: Any): List[T] = {
    val futures: List[Future[T]] = geos.map(a => (a ? msg).mapTo[T]).toList
    futures.map(Await.result(_, defaultDuration))
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