
import java.io.BufferedReader
import java.io.InputStreamReader
import oauth.signpost._
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http._
import org.apache.http.entity.StringEntity
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.impl.client.DefaultHttpClient
import akka.actor.Actor

/**
 * A simple example of the streaming API based on specific keyword searches.
 * Simply print the text content of the tweets.
 *
 * @see TweetStreamemerExampleSpec to launch it.
 * 		command : play testOnly TweetStreamerExampleSpec
 * @pre Add the required keys in the conf. Subscribe : https://apps.twitter.com/
 */
class TweetStreamExampleActorClement extends Actor {
   val consumerKey = "pjc4NI7o716dIB9GZprwQ"
   val consumerSecret = "hRt9cqycdxs1VHmYLy0tgyOQyUPqQ9jaWpy8Oj7I"
   val accessToken = "97033937-37pmIh0wgFoM6Yt6R9Adg43I2wka1WuolBwRXbfS4"
   val accessTokenSecret = "fR73AeZCY0U88VOWihtMa3c3Pg0bqs9nUDTwgI11EmIEq"

   //We set up the HTTP client and the oauth module
   val oauthConsumer = new CommonsHttpOAuthConsumer(
        		consumerKey,
        		consumerSecret
                )
   val client = new DefaultHttpClient()


  def receive = {
    case strs: Array[String] => 
      println("Launching the streamer...")

      oauthConsumer.setTokenWithSecret(accessToken, accessTokenSecret)
     
      //We construct the HTTP request
      val post = new HttpPost("https://stream.twitter.com/1.1/statuses/filter.json") 
      var parameter = "" //We construct the parameter
      var first = true
      for ( currentWord <- strs ) {
         if (!first)
           parameter += ','
         else
           first = false
         parameter += currentWord
      }

      //We set the parameters in a weird way that fits with SCALA/Apache HTTP compatibilities
      val params = new java.util.ArrayList[BasicNameValuePair](1)
      params.add(new BasicNameValuePair("track",parameter))
      post.addHeader("Content-Type", "application/x-www-form-urlencoded")
      post.setEntity(new UrlEncodedFormEntity(params))

      oauthConsumer.sign(post) //We sign our POST http request using oauth specification

      def twitterResponse = client.execute(post) //We send the request and get the response
      def inputStreamResponse = twitterResponse.getEntity().getContent() //We extract an inputstream object from the response


      //Then we have the inputstream. We can print it with, for example : 
      var in = new BufferedReader(new InputStreamReader(inputStreamResponse))

      while (true)
          println(in.readLine())

      in.close()
    

    case _ => sys.error("Not a valid input for the Streamer Example !")
  }
}
