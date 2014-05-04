package controllers

import scala.collection.mutable.MutableList

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
  val geoParts: MutableList[ActorRef] = MutableList()
  
  def start() = {
    //TODO: get form data from cache
    //TODO: start a tweetmanager searching for the data
    //TODO: how to choose grid size?
    //TODO: error handling

    val squares = Cache.getAs[List[(Double, Double, Double, Double)]]("squares").get

    // Get keywords and translations
    val keywordsList = Cache.getAs[List[(String, List[String])]]("fromTimoToJorisAndLewis").get // list of tuple (initialKeyword, translations&synonyms)
    val startKey: List[String] = keywordsList.map(_._1)
    val otherKey: List[String] = keywordsList.map(_._2).flatten
    val allKeywords = (startKey ++ otherKey).distinct

    
    for (square <- squares) {
      val geoSquare = GeoSquare(square._1, square._2, square._3, square._4)
      val geoPart: ActorRef = Props(new GeoPartitionner(allKeywords, geoSquare, 10, 10))
      geoParts += geoPart
      geoPart ! StartGeo
    }
    TweetManagerRef ! Start

    Ok
  }

  def pause = {
    //TODO: stop the tweetmanager
    Ok
  }

  def resume = {
    //TODO: start the tweetmanager again
    Ok
  }

  def computeDisplayData = {
    //TODO: opacity
    //TODO: Venn diagram
    //TODO: clustering
    Ok
  }

}

object Gathering extends GatheringController with Controller
