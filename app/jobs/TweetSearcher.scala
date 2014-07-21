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
import utils.Http._
import models._
import play.api.libs.json.JsResultException
import utils.AkkaImplicits._
import utils.Enrichments._
import java.util.concurrent.TimeUnit
import scala.util.Random
import akka.actor.Cancellable
import play.Logger
import java.io.{PrintWriter, File}


/**
 * Request tweets to the API and send them to the good listener once a result is received and validated.
 * API used: https://dev.twitter.com/docs/api/1.1/get/search/tweets
 * 
 * A TweetSearcher implements a round-ronbin scheme on all its TweetQueries. Moreover, if a TweetQuery has
 * the maximum number of result, this one is scheduled twice to avoid loosing potential tweet during the next
 * ping. The list is then reshuffled. Based on that, the areas with city / more active accounts will receive
 * more attention from the Searcher as it discovers them.
 * 
 * @pre All the queries added are already subdivided.
 */
class TweetSearcher(qList: List[(TweetQuery, ActorRef)], checker: ActorRef, searchRate: Int, twitterKey: Int, manager: ActorRef) extends Actor {

  var running = true /* If true, the actor will be scheduled */
  var scheduled: Option[Cancellable] = None /* cancellable corresponding to the self-schedule of the thread */
  
  var next = 0 /* Keep track of the next query to be launched */
  /* Map between Integers and TweetQuery, corresponding listener and potential callback URL */
  var qurs: Map[Int, (TweetQuery, ActorRef, Option[String])] = qList.zipWithIndex.map(e => (e._2, (e._1._1, e._1._2, None))) toMap

  val file = new PrintWriter(new File("tweets/searcher" + this.##.toString + ".txt"))

  def receive = {
    
    case Ping =>
      Logger.info("TweetSearcher: asking for some tweets...") /* TODO: remove, there just for tests */
      /* Contact the Twitter API and ask for tweet. If the query was alread done before, just ask for updates. */
      val (query, listener, callback) = qurs(next)
      try {

        /* Set up the parameter of the query's URL. */
        val params = callback.getOrElse(s"?geocode=${query.area.center._2},${query.area.center._1},${query.area.radius}km&q=${query.kwsInSearchFormat}&result_type=recent&count=100")
        val queryString = s"https://api.twitter.com/1.1/search/tweets.json${params}"

        /* Ask Twitter, get all the result, and close the Stream */
        val stream = askForGet(queryString, Some(consumer(twitterKey)))
        val retString = readAll(stream)
        stream.close
        
        /* Parse the values and send them to the approriate listener */ 
        val (values, clb) = parseJson(retString)
        qurs += (next -> (query, listener, Some(clb))) /* Update the list with the callback value */
        if(values.size == 100) { /* If the maximum possible number of tweets is received, schedule the query once more. */ 
          qurs += (qurs.size -> (query, listener, Some(clb)))
          qurs = qurs.toList.map(e => e._2).shuffle.zipWithIndex.map(_.swap).toMap
        }

        /* printout all the tweets */
        values foreach {v =>
          file.write(v.toString + "\n")
          file.flush()
        }

        /* Ask the checker to filter duplicates and to send the good tweets to the listener */
        values foreach (v => checker ! (Tweet(v, query), listener))

      } catch {
        case _: JsResultException => 
          Logger.info("TweetSearcher: error while parsing Json...(probably rejected by the Twitter API, should work again soon).")
          qurs += (next -> (query, listener, None)) /* Remove the callbacks. Since we were refused, the queries will start blank again. */
          manager ! Refused
        case _: javax.net.ssl.SSLPeerUnverifiedException => 
          Logger.info("TweetSearcher: error while authenticating with Twitter (probably just a little network error, nothing to worry about).")
        case _: Exception =>
          Logger.info("TweetSearcher: api.twitter.com not found (DNS server or your internet connection down).")
      } finally {
        next = (next + 1) % qurs.size /* Round-robin */
        if (running) scheduled = Some(self.scheduleOnce(searchRate, TimeUnit.SECONDS, Ping)) /* Auto schedule once more */
      }

    case Wait => /* Message sent by the TweetManager, notifying the TweetSearcher that a request to the API has been refused somewhere. */
      if (running) {
        if (scheduled.isDefined) scheduled.get.cancel /* Cancel the auto schedule */
        /* Re-schedule the Ping later, waiting some time to avoid congestion and being re-blocked by the API. */
        val timeToWait = getConfInt("tweetManager.waitOnRefusal", "TweetSearcher: cannot find the tweetManager.waitOnRefusal key.")
        scheduled = Some(self.scheduleOnce(timeToWait + new Random(System.currentTimeMillis).nextInt(searchRate), TimeUnit.SECONDS, Ping))
      }

    case Stop => 
      running = false
      if (scheduled.isDefined) scheduled.get.cancel

    case Resume =>
      running = true
      scheduled = Some(self.scheduleOnce(new Random(System.currentTimeMillis).nextInt(searchRate), TimeUnit.SECONDS, Ping))

    case _ => sys.error("Not a valid input for the Tweer Searcher!")
  }

  /** 
   *  Parse a string into a list of JsValue and a callback String from the Twitter Json 
   *  
   *  @param str		The String to parse
   *  @return A tuple of list of Tweets as JsValues and a call back URL as String 
   */
  def parseJson(str: String): (Array[JsValue], String) = {
    val glb = Json.parse(str.toString)
    (((glb \ "statuses").as[Array[JsValue]]), (glb \ "search_metadata" \ "refresh_url").as[JsString].value)
  }
}
