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

/**
 * Gets a stream of tweets from the Streaming API
 * https://dev.twitter.com/docs/api/1.1/post/statuses/filter
 * Max #request/15minutes: 450, Max #keywords=10
 */
class TweetStreamer(query: List[(TweetQuery, ActorRef)]) extends Actor {
  /* We set up the HTTP client and the oauth module */
  val client = new DefaultHttpClient()

  var running = true
  var scheduled: Option[Cancellable] = None

  var stream: InputStream = null

  var squareToListener: HashMap[String, List[(TweetQuery, ActorRef)]] = HashMap()
  var locationParam = ""
  var setOfKeywords: List[Array[String]] = Nil
  
  var containsOneOfTheKeywords = false
  
  def receive = {
    case Start => /* First execution */
      setOfKeywords = parseRequest(query).map(currentString => currentString.split(","))
      stream = askFor("https://stream.twitter.com/1.1/statuses/filter.json", locationParam)
      readAll(stream)

    case Ping => /* Callback execution (query update) */
      if (stream == null)
        receive(Start)
      else
        readAll(stream)
      /* TODO: set the searchRate properly */
      if (running) scheduled = Some(self.scheduleOnce(3, TimeUnit.SECONDS, Ping)) /* Auto schedule once more */
    
    case Stop => 
      if(scheduled.isDefined) scheduled.get.cancel
      running = false
      if (stream != null)
        stream.close()

    case _ => sys.error("Not a valid input for the Tweet Streamer!")
  }
  
  /**
   * Returns a list of keywords, updates the list squareToListener that gives us for each geosquare 
   * a listener and updates the locationparam
  */
  def parseRequest(requests: List[(TweetQuery, ActorRef)]): List[String] = {
    var listKeyWords: List[String] = Nil // The list of all the sets of keywords, because there will be more than one
    
    //We want to do a search on a big square, we are going to degine it's bounds
    var inferiorBounds: (Double, Double) = (361,361)
    var superiorBounds: (Double, Double) = (-361,-361)
    
    //We parse the list
    var nextCouple: (TweetQuery, ActorRef) = null
    var bottomList: List[(TweetQuery, ActorRef)] = requests
    
    while (bottomList != Nil){
      nextCouple = bottomList.head
      val currentHashKeywords = hashKeyWords(nextCouple._1.keywords) //we do a hash for our keywords
      //We add it in the right place on the hashmap
      if (squareToListener.contains(currentHashKeywords)){
        squareToListener(currentHashKeywords) :+= (nextCouple._1, nextCouple._2) //We remember each of the geosquare to give tweets to the right listener
      }
      else{
        var newList: List[(TweetQuery, ActorRef)] = Nil
        newList :+= (nextCouple._1, nextCouple._2)
        squareToListener.put(currentHashKeywords, newList)
      }
      //We try to add te set of keywords to 
      
      if (!listKeyWords.contains(currentHashKeywords)){
        listKeyWords :+= currentHashKeywords
      }
      
      //Then we update the bounds
      val currentSquare: GeoSquare = nextCouple._1.area
      
      if (currentSquare.long1 < inferiorBounds._1)
        inferiorBounds = (currentSquare.long1, inferiorBounds._2)
      if (currentSquare.lat1 < inferiorBounds._2)
        inferiorBounds = (inferiorBounds._1, currentSquare.lat1)
      
      if (currentSquare.long2 > superiorBounds._1)
        superiorBounds = (currentSquare.long2, superiorBounds._2)
      if (currentSquare.lat2 > superiorBounds._2)
        superiorBounds = (superiorBounds._1, currentSquare.lat2)
      
      
      bottomList = bottomList.tail
    }
    
    //And we return a list of geoparameters for each list of keywords
    locationParam = inferiorBounds._1 + "," + inferiorBounds._2 + "," + superiorBounds._1 + "," + superiorBounds._2
    listKeyWords
  }
  
  def hashKeyWords(listOfWords: List[String]): String = listOfWords match {
    case string :: Nil => string
    case string :: bottom => string + "," + hashKeyWords(bottom)
    
    case Nil => ""
  }
  
  /**
   * Execute the request in parameter, parse it and send the tweets to the listener.
   * We have to send the tweets to the right listener
   */
 /* def sendToListener() = {
    val ret = readAll(stream)
    val values = parseJson(ret)
    listener ! Tweet(values, query)
  }*/

  def askFor(request: String, location: String) = {
    val postRequest = new HttpPost(request)

    //We set the parameters in a weird way that fits with SCALA/Apache HTTP compatibilities
    val params = new java.util.ArrayList[BasicNameValuePair](1)
    //params.add(new BasicNameValuePair("track",keywords))
    params.add(new BasicNameValuePair("locations",location))
    postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded")
    postRequest.setEntity(new UrlEncodedFormEntity(params))

    consumer().sign(postRequest)

    val twitterResponse = client.execute(postRequest) /* Send the request and get the response */
    twitterResponse.getEntity().getContent()
  }

  /** 
   *  Tells us if this tweet match our request of words
   */
  def sendToListenerIfCorrect(currentJSon: String, currentSetOfwords: Array[String]){
    //We make a hash of the keyword sot search in the hashmap
    val hashCurrentSet = hashKeyWords(currentSetOfwords.toList) 
    //Then we begin by extracting the "test" field to see if we find the words we are looking for
    var textArray = currentJSon.split("\"text\":\"")
    if (textArray.size > 1){
	  textArray = textArray.apply(1).split("\",")
	  if (textArray.size > 1){
	    val text = textArray.apply(0)
	    //We check for each word if the tweet contains if
        for ( i <- 0 to (currentSetOfwords.length - 1)){
          if (text.contains(currentSetOfwords.apply(i))){
            //We send it to the correct listener by looking on the hashmap
            var currentListenersOfThisTweet: List[(TweetQuery, ActorRef)] = squareToListener(hashCurrentSet) //We take the hash for our set of keywords
            var currentGeoSquare: (TweetQuery, ActorRef) = null
            
            val jsonToSend: JsValue = parseJson(currentJSon)
            var geoJS: JsValue = jsonToSend \ "geo"
            
   	        if (geoJS.toString != "null"){
	          geoJS = geoJS \ "coordinates"
	          while (currentListenersOfThisTweet != Nil && !containsOneOfTheKeywords){
	            currentGeoSquare = currentListenersOfThisTweet.head
	            if (currentGeoSquare._1.area.containsGeo(geoJS.apply(1).as[Double], geoJS.apply(0).as[Double])){
	              //println("Got one tweet from tweet streamer " + text + geoJS.apply(0).as[Double] + ' ' + geoJS.apply(1).as[Double])
	              currentGeoSquare._2 ! Tweet(jsonToSend, currentGeoSquare._1)
	              containsOneOfTheKeywords = true
	            }
	            currentListenersOfThisTweet = currentListenersOfThisTweet.tail
	          }
            }
          }
        }
      }
    }
  }
  
  
  /** Read all the data present in the InputStream in and return it as a String */
  def readAll(in: InputStream) = {
    val inr = new BufferedReader(new InputStreamReader(in))
    val bf = new StringBuilder
    var rd = inr.readLine
    var value = 0
    
    while (!containsOneOfTheKeywords) { 
      val currentJSon = rd
      if (currentJSon != null){
        setOfKeywords.foreach((arrayKeyWords:Array[String]) => sendToListenerIfCorrect(currentJSon, arrayKeyWords))
        rd = inr.readLine 
      }
    }
    containsOneOfTheKeywords = false
  }

  /** Parse a string into a list of JsValue and a callback String from the Twitter Json */
  def parseJson(str: String): JsValue = {
    val glb = Json.parse(str.toString)
    //println(glb)
    glb
  }
} 
