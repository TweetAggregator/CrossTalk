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
import models.TweetListener
import org.apache.http.client.methods.HttpGet
import play.api.libs.json.Json
import models.Tweet
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue
import play.api.Play
import play.api.libs.json.JsString

/**
 * Launch a research on Tweets and notify the good listener once a result is received.
 * https://dev.twitter.com/docs/api/1.1/get/search/tweets
 * Max #request/15minutes: 450, Max #keywords=10
 */
class TweetSearcher extends Actor {
  val consumerKey = Play.current.configuration.getString("twitter.consumerKey").getOrElse(sys.error("No consumer key found in conf."))
  val consumerSecret = Play.current.configuration.getString("twitter.consumerSecret").getOrElse(sys.error("No consumer secret found in conf."))
  val accessToken = Play.current.configuration.getString("twitter.accessToken").getOrElse(sys.error("No access token found in conf."))
  val accessTokenSecret = Play.current.configuration.getString("twitter.accessTokenSecret").getOrElse(sys.error("No access token secret found in conf."))

  /* We set up the HTTP client and the oauth module */
  val oauthConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret)
  oauthConsumer.setTokenWithSecret(accessToken, accessTokenSecret)
  val client = new DefaultHttpClient()

  var continue = true /* Set to true if the tweetSearch is allowed to do call backs */
  def exec(query: TweetQuery, listener: TweetListener, period: Int, oldReq: Option[String]):Unit = if (continue) {

    /* Construct the HTTP request */
    val req = oldReq match {
      case None =>
        /* latitude/longitude, inverted from the Streaming API */
        val geoParams = query.area.center._2 + "," + query.area.center._1 + "," + query.area.radius + "km"
        val keywordsParams = query.keywords.mkString(",")
        new HttpGet("https://api.twitter.com/1.1/search/tweets.json?geocode=" + geoParams + "&q=" + keywordsParams + "&result_type=recent&count=100")
      case Some(old) => new HttpGet("https://api.twitter.com/1.1/search/tweets.json" + old)
    }
    req.addHeader("Content-Type", "application/x-www-form-urlencoded")

    oauthConsumer.sign(req) /* We sign our Get http request using oauth specification */

    def twitterResponse = client.execute(req) /* Send the request and get the response */
    def inputStreamResponse = twitterResponse.getEntity().getContent() /* Extract an InputStream object from the response */

    /* Get all the answer as string */
    val in = new BufferedReader(new InputStreamReader(inputStreamResponse))
    val bf = new StringBuilder
    var rd = in.readLine
    while (rd != null) { bf.append(rd); rd = in.readLine }
    
    /* Parse result + callback url */
    val glb = Json.parse(bf.toString)
    ((glb \ "statuses").as[Array[JsValue]]).foreach(status => listener.act(Tweet(status, query.area)))
    val old = (glb \ "search_metadata" \ "refresh_url").as[JsString]
    in.close()
    
    /* Sleep to avoid congestion and call back the API */
    Thread.sleep(period)
    exec(query, listener, period, Some(old.value))
  }

  def receive = {
    /* query: The queries to execute, listener: the listener to notify, period: miliseconds */
    case (query: TweetQuery, listener: TweetListener, period: Int) => exec(query, listener, period, None)
    case "terminate" => continue = false
    case _ => sys.error("Not a valid pair of (query,listener, period(int)) from the Tweer Searcher!")
  }
}