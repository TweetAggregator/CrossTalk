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

  val listenersOfAWord = new HashMap[String, List[(TweetQuery, ActorRef)] ]()
  val probaOfAWord = new HashMap[String, Int]()
  val newRandom = new Random(System.currentTimeMillis)
  /**
   * Number of maximum number of tweets 
   */
  val maxValue = query.size
  
  def receive = {
    case Start => 
      computeKeyWordRandomImportance(query)
      running = true
      scheduled = Some(self.scheduleOnce(waitToExplore, TimeUnit.MILLISECONDS, Ping)) /* Auto schedule once more */
      
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
  /**
   * This function computes the listener of each st of words and the associated frequency for ach of them. 
   * This fequency is random at the begining but constant over time
   */
  def computeKeyWordRandomImportance(queries: List[(TweetQuery, ActorRef)]) = {
    var nextCouple: (TweetQuery, ActorRef) = null
    var bottomList: List[(TweetQuery, ActorRef)] = queries
    
    while (bottomList != Nil) {
      nextCouple = bottomList.head
      val currentHashKeywords = hashKeyWords(nextCouple._1.keywords)
      if (listenersOfAWord.contains(currentHashKeywords)){
        listenersOfAWord(currentHashKeywords) :+= (nextCouple._1, nextCouple._2)
      }
      else{
        var newList: List[(TweetQuery, ActorRef)] = Nil
        newList :+= (nextCouple._1, nextCouple._2)
        listenersOfAWord.put(currentHashKeywords, newList)
        probaOfAWord.put(currentHashKeywords, newRandom.nextInt(maxValue) + 1)
      }
      bottomList = bottomList.tail
    }
  }
  
  def createRandomNewTweet(): String  = {
    "{\"text\":\"" + newRandom.nextInt() + "\"}"
  }
  
  def feedListeners(){
    for (currentWord: (String, List[(TweetQuery, ActorRef)]) <- listenersOfAWord){
      var i = probaOfAWord(currentWord._1)
      var next = (newRandom.nextInt( (currentWord._2.size) ) + 2) / 2 
      
      while (i > 0){
        for (currentListener: (TweetQuery, ActorRef) <- currentWord._2){
          if (next == 0){
        	currentListener._2 ! (new Tweet(Json.parse(createRandomNewTweet()), currentListener._1))
          	i -= 1
          	next = (newRandom.nextInt(currentWord._2.size) + 2) / 2
          }
          next -= 1
        }
      }
    }
  }
  
  def hashKeyWords(listOfWords: List[String]): String = listOfWords match {
    case string :: Nil => string
    case string :: bottom => string + "," + hashKeyWords(bottom)

    case Nil => ""
  }
  
} 
