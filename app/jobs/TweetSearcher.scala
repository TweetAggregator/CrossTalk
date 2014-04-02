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
/**
 * Launch a research on Tweets and send them to the good listener once a result is received.
 * https://dev.twitter.com/docs/api/1.1/get/search/tweets
 * Max #request/15minutes: 450, Max #keywords=10
 */
class TweetSearcher(query: TweetQuery, listener: TweetListener) extends Actor {

  /* We set up the HTTP client and the oauth module */
  val oauthConsumer = TweetManager.getConsumer
  val client = new DefaultHttpClient()

  var callback: Option[String] = None /* Store the params used to check for updates */
  var olderResults: Option[String] = None /* Store the params used to check for older results */

  def receive = {
    case "start" => /* First execution */
      val geoParams = query.area.center._2 + "," + query.area.center._1 + "," + query.area.radius + "km"
      val keywordsParams = query.keywords.mkString(",")
      val stream = askFor("https://api.twitter.com/1.1/search/tweets.json?geocode=" + geoParams + "&q=" + keywordsParams + "&result_type=recent&count=100")
      val ret = readAll(stream)
      stream.close
      val parsed = parseAndUpdateCalls(ret)
      parsed foreach (listener ! Tweet(_, query))

    case "callback" => /* Callback execution (query update) */
      if (callback.isEmpty) sys.error("Query on an TweetSearcher not yet started.")
      val stream = askFor("https://api.twitter.com/1.1/search/tweets.json" + callback.get)
      val ret = readAll(stream)
      stream.close
      val parsed = parseAndUpdateCalls(ret)
      parsed foreach (listener ! Tweet(_, query))

    case "getOlder" => /* return the older result than the one previously returned */
      if (olderResults.isEmpty) sys.error("Query on an TweetSearcher not yet started.")
      val stream = askFor("https://api.twitter.com/1.1/search/tweets.json" + olderResults.get)
      val ret = readAll(stream)
      stream.close
      val parsed = parseAndUpdateCalls(ret)
      parsed foreach (listener ! Tweet(_, query))

    case _ => sys.error("Not a valid input for the Tweer Searcher!")
  }

  /** Ask for the request passed in parameter, signed by the oauthConsumer. */
  def askFor(request: String) = {
    val HttpRequest = new HttpGet(request)
    HttpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded")

    oauthConsumer.sign(HttpRequest)

    val twitterResponse = client.execute(HttpRequest) /* Send the request and get the response */
    twitterResponse.getEntity().getContent()
  }

  /** Read all the data present in the InputStream in and return it as a String */
  def readAll(in: InputStream): String = {
    val inr = new BufferedReader(new InputStreamReader(in))
    val bf = new StringBuilder
    var rd = inr.readLine
    while (rd != null) { bf.append(rd); rd = inr.readLine }
    bf.toString
  }

  /** Parse a string into a list of JsValue and a callback String from the Twitter Json */
  def parseAndUpdateCalls(str: String): Array[JsValue] = {
    /* Parse result + callback url */
    val glb = Json.parse(str.toString)
    callback = Some((glb \ "search_metadata" \ "refresh_url").as[JsString].value)
    olderResults = Some((glb \ "search_metadata" \ "next_results").as[JsString].value)
    ((glb \ "statuses").as[Array[JsValue]])
  }
}