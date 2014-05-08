package jobs

import akka.actor.Actor
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import models.TweetQuery
import org.apache.http.client.methods.HttpGet
import play.api.libs.json.Json
import models.Tweet
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue
import play.api.Play
import play.api.libs.json.JsString
import akka.actor.ActorRef
import TweetManager._
import java.io.InputStream
import models.GeoSquare
import play.api.libs.json.JsResultException
import collection.mutable.HashMap
import akka.actor.Cancellable
import models._
import utils.AkkaImplicits._
import utils.Enrichments._
import java.util.concurrent.TimeUnit
import scala.util.Random

/**
 * Gets a stream of tweets from the Streaming API
 * https://dev.twitter.com/docs/api/1.1/post/statuses/filter
 * Max #request/15minutes: 450, Max #keywords=10
 */
class TweetTester(query: List[(TweetQuery, ActorRef)]) extends Actor {
  /* We set up the HTTP client and the oauth module */
  val client = new DefaultHttpClient()

  var running = true
  var scheduled: Option[Cancellable] = None

  /* Time the Streamer has to wait before exploring the stream again once a matching tweet found. */
  val waitToExplore = getConfInt("tweetTester.waitToExplore", "TweetStreamer: key waitToExplore not defined in conf.")


  def receive = {
    case Start => 
      if (running) scheduled = Some(self.scheduleOnce(waitToExplore, TimeUnit.MILLISECONDS, Ping)) /* Auto schedule once more */

    case Ping => /* Callback execution (query update) */
       feedListeners()
       if (running) scheduled = Some(self.scheduleOnce(waitToExplore, TimeUnit.MILLISECONDS, Ping)) /* Auto schedule once more */

    case Stop =>
      running = false
      if (scheduled.isDefined) scheduled.get.cancel

    case Pause =>
      receive(Stop)

    case Resume =>
      receive(Start)

    case Wait => /* Nothing to do, a tester continue forever. This is just for TweetSearcher. */

    case _ => sys.error("Not a valid input for the Tweet Streamer!")
  }

  def createRandomNewTweet(): String  = {
    val newRandom = new Random()
    "{\"text\":\"" + newRandom.nextInt() + "\"}"
  }
  
  def feedListeners(){
    for (currentTuple: (TweetQuery, ActorRef) <- query)
      currentTuple._2 ! (new Tweet(Json.parse(createRandomNewTweet()), currentTuple._1))
  }
} 