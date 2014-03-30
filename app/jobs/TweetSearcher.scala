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

/**
 * Launch a research on Tweets and notify the good listener once a result is received.
 * https://dev.twitter.com/docs/api/1.1/get/search/tweets
 * Max #request/minutes: 450
 */
class TweetSearcher extends Actor {
  val consumerKey = "pjc4NI7o716dIB9GZprwQ"
  val consumerSecret = "hRt9cqycdxs1VHmYLy0tgyOQyUPqQ9jaWpy8Oj7I"
  val accessToken = "97033937-37pmIh0wgFoM6Yt6R9Adg43I2wka1WuolBwRXbfS4"
  val accessTokenSecret = "fR73AeZCY0U88VOWihtMa3c3Pg0bqs9nUDTwgI11EmIEq"

  /* We set up the HTTP client and the oauth module */
  val oauthConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret)
  val client = new DefaultHttpClient()

  def receive = {
    /* query: The queries to execute, listener: the listener to notify, period: #request/minutes */
    case (query: TweetQuery, listener: TweetListener, period: Int) =>

      oauthConsumer.setTokenWithSecret(accessToken, accessTokenSecret)

      /* Construct the HTTP request */
      
      /* latitude/longitude, inverted from the Streaming API */
      val geoParams = query.area.center._2 + "," + query.area.center._1 + "," + query.area.radius + "km" 
      val keywordsParams = query.keywords.mkString(",")
      val get = new HttpGet("https://api.twitter.com/1.1/search/tweets.json?geocode=" + geoParams + "&q=" + keywordsParams + "&result_type=recent&count=100")
      get.addHeader("Content-Type", "application/x-www-form-urlencoded")

      oauthConsumer.sign(get) /* We sign our Get http request using oauth specification */

      def twitterResponse = client.execute(get) /* We send the request and get the response */
      def inputStreamResponse = twitterResponse.getEntity().getContent() /* We extract an inputstream object from the response */

      /* TODO: output to the listener */
      var in = new BufferedReader(new InputStreamReader(inputStreamResponse))
      while(true) {
        val rd = in.readLine()
        if (rd != null) println(rd)
      }

      in.close()

    case _ => sys.error("Not a valid pair of (query,listener, period(int)) from the Tweer Searcher!")
  }
}