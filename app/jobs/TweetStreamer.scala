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
/**
 * Launch a research on Tweets and send them to the good listener once a result is received.
 * https://dev.twitter.com/docs/api/1.1/get/search/tweets
 * Max #request/15minutes: 450, Max #keywords=10
 */
class TweetStreamer(query: TweetQuery, listener: TweetListener) extends Actor {

  /* We set up the HTTP client and the oauth module */
  val oauthConsumer = TweetManager.getConsumer
  val client = new DefaultHttpClient()

  var callback: Option[String] = None /* Store the params used to check for updates */

  var stream: InputStream = null

  def receive = {
    case "start" => /* First execution */
      val locationParam = query.area.long1 + "," + query.area.lat1 + "," + query.area.long2 + "," + query.area.lat2
      val keywordsParam = query.keywords.mkString(",")
      stream = askFor("https://stream.twitter.com/1.1/statuses/filter.json", keywordsParam, locationParam)
      sendToListener()

    case "callback" => /* Callback execution (query update) */
      callback match {
        case Some(properties) => sendToListener()
        case None =>
          /* A parsing error occurred or our searcher has been kicked by the API, restarting... */
          receive("start")
      }

    case _ => sys.error("Not a valid input for the Tweer Searcher!")
  }

  /**
   * Execute the request in parameter, parse it and send the tweets to the listener.
   */
  def sendToListener() = {
    val ret = readAll(stream)
    try {
      val (values, clb) = parseJson(ret)
      callback = Some(clb)
      values foreach (listener ! Tweet(_, query))
    } catch {
      case _: JsResultException => /* Error while parsing (Rate limit exceeded) */
        callback = None /* Will restart the searcher next time it receive a callback */
    }
  }

  def askFor(request: String, keywords: String, location: String) = {
    val postRequest = new HttpPost(request)

    //We set the parameters in a weird way that fits with SCALA/Apache HTTP compatibilities
    val params = new java.util.ArrayList[BasicNameValuePair](2)
    params.add(new BasicNameValuePair("track",keywords))
    params.add(new BasicNameValuePair("location",location))
    postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded")
    postRequest.setEntity(new UrlEncodedFormEntity(params))

    oauthConsumer.sign(postRequest)

    val twitterResponse = client.execute(postRequest) /* Send the request and get the response */
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
  def parseJson(str: String): (Array[JsValue], String) = {
    val glb = Json.parse(str.toString)
    (((glb \ "statuses").as[Array[JsValue]]), (glb \ "search_metadata" \ "refresh_url").as[JsString].value)
  }
}
