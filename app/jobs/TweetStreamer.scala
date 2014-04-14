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
import models._

/**
 * Gets a stream of tweets from the Streaming API
 * https://dev.twitter.com/docs/api/1.1/post/statuses/filter
 * Max #request/15minutes: 450, Max #keywords=10
 */
class TweetStreamer(query: TweetQuery, listener: ActorRef) extends Actor {

  /* We set up the HTTP client and the oauth module */
  val client = new DefaultHttpClient()

  var callback: Option[String] = None /* Store the params used to check for updates */

  var stream: InputStream = null

  def receive = {
    case Start => /* First execution */
      val locationParam = query.area.long1 + "," + query.area.lat1 + "," + query.area.long2 + "," + query.area.lat2
      stream = askFor("https://stream.twitter.com/1.1/statuses/filter.json", locationParam)
      sendToListener()

    case Ping => /* Callback execution (query update) */
      if (stream == null)
        receive(Start)
      else
        sendToListener()
      

    case _ => sys.error("Not a valid input for the Tweer Streamer!")
  }

  /**
   * Execute the request in parameter, parse it and send the tweets to the listener.
   */
  def sendToListener() = {
    val ret = readAll(stream)
    val values = parseJson(ret)
    listener ! Tweet(values, query)
  }

  def askFor(request: String, location: String) = {
    val postRequest = new HttpPost(request)

    //We set the parameters in a weird way that fits with SCALA/Apache HTTP compatibilities
    val params = new java.util.ArrayList[BasicNameValuePair](1)
    //params.add(new BasicNameValuePair("track",keywords))
    params.add(new BasicNameValuePair("locations",location))
    postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded")
    postRequest.setEntity(new UrlEncodedFormEntity(params))

    consumer.sign(postRequest)

    val twitterResponse = client.execute(postRequest) /* Send the request and get the response */
    twitterResponse.getEntity().getContent()
  }

  /** Read all the data present in the InputStream in and return it as a String */
  def readAll(in: InputStream): String = {
    val inr = new BufferedReader(new InputStreamReader(in))
    val bf = new StringBuilder
    var rd = inr.readLine
    var value = 0
    var containsOneOfTheKeywords = false
    while (!containsOneOfTheKeywords) { 
      val currentJSon = rd
      if (currentJSon != null){
        val text = currentJSon.split("\"text\":\"").apply(1).split("\",").apply(0)
        
        for ( i <- 0 to (query.keywords.length - 1)){
           if (text.contains(query.keywords.apply(i)))
             containsOneOfTheKeywords = true
        }
        if (containsOneOfTheKeywords){
          bf.append(currentJSon)
        }
        rd = inr.readLine 
      }
    }
    bf.toString
  }

  /** Parse a string into a list of JsValue and a callback String from the Twitter Json */
  def parseJson(str: String): JsValue = {
    val glb = Json.parse(str.toString)
    glb
  }
}
