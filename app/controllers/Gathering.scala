package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

/**
 * Gathering the tweets and processing the results
 */
trait GatheringController { this: Controller =>
  def start = {
    //TODO: get form data from cache
    //TODO: start a tweetmanager searching for the data
    //TODO: get translations
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
