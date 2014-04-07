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

/**
 * Launch a research on Tweets and send them to the good listener once a result is received.
 * https://dev.twitter.com/docs/api/1.1/get/search/tweets
 * Max #request/15minutes: 450, Max #keywords=10
 */
class TweetSearcher(query: TweetQuery, listener: ActorRef) extends Actor {

  var callback: Option[String] = None /* Store the params used to check for updates */

  def receive = {
    case "start" => /* First execution */
      val geoParams = query.area.center._2 + "," + query.area.center._1 + "," + query.area.radius + "km"
      execRequest("https://api.twitter.com/1.1/search/tweets.json?geocode=" + geoParams + "&q=" + query.kwsInSearchFormat + "&result_type=recent&count=100")

    case "callback" => /* Callback execution (query update) */
      callback match {
        case Some(properties) =>
          execRequest("https://api.twitter.com/1.1/search/tweets.json" + properties)
        case None =>
          /* A parsing error occurred or our searcher has been kicked by the API, restarting... */
          receive("start")
      }

    case _ => sys.error("Not a valid input for the Tweer Searcher!")
  }

  /**
   * Execute the request in parameter, parse it and send the tweets to the listener.
   */
  def execRequest(request: String) = {
    val stream = askForGet(request, Some(consumer))
    val ret = readAll(stream)
    stream.close
    try {
      val (values, clb) = parseJson(ret)
      callback = Some(clb)
      values foreach (listener ! Tweet(_, query))
    } catch {
      case _: JsResultException => /* Error while parsing (Rate limit exceeded) */
        callback = None /* Will restart the searcher next time it receive a callback */
    }
  }

  /** Parse a string into a list of JsValue and a callback String from the Twitter Json */
  def parseJson(str: String): (Array[JsValue], String) = {
    val glb = Json.parse(str.toString)
    (((glb \ "statuses").as[Array[JsValue]]), (glb \ "search_metadata" \ "refresh_url").as[JsString].value)
  }
}
