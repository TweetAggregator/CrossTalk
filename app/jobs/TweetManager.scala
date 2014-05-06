package jobs

import akka.actor.Actor
import play.api.Play
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import akka.actor.ActorRef
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import models._
import akka.actor.Cancellable
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import TweetManager._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import utils.AkkaImplicits._
import utils.Enrichments._
import play.libs.Akka
import scala.util.Random

/**
 * Manage all the tweets streamer to return from the two APIs, and ensure that the quotas are enforced.
 * Since we want a dynamic update, the receive method can execute various queries:
 *  1		Add queries to the list to start
 *  2		Start the queries added
 *  3		Stop all the queries
 *  4   Pause all the queries
 *  5   Resume all the queries
 *
 *  Note that there should be only one instance of the manager per application, hence the nesting with the companion object.
 *  @pre All the queries added are already subdivided.
 *  @pre All queries added using one call to AddQuery are contiguous. This is a constrains to use the TweetStreamer.
 */
object TweetManager {

  /* Nested class to enforce singleton */
  class TweetManager extends Actor {
    
    /** Period between two ping to the Twitter API for the tweet/search API. */
    val period = getConfInt("tweetManager.period", "TweetManager: no period specified for the connection with Twitter.")
    /** limit on the number of Akka Actor allowed for the research of the tweet/search API. Queries will be split among them. */
    val searcherBound = getConfInt("tweetManager.searcherBound", "TweetManager: no bound specified on the number of searchers.")

    /** A list of actors launched by the Manager. */
    var actorRefs: List[ActorRef] = Nil
    /** A list of queries to start. Only used prior to the start of the Manager. */
    var queriesToStart: List[List[(TweetQuery, ActorRef)]] = Nil

    def receive = {

      case AddQueries(queries) =>
        assert(actorRefs.isEmpty, "TweetManager: Cannot add more queries without cancelling the ones running.")
        queriesToStart :+= queries
        sender ! Done

      case Start =>
        assert(actorRefs.isEmpty, "TweetManager: Cannot restart queries again without cancelling the ones running.")
        assert(!queriesToStart.isEmpty, "TweetManager: Cannot start no query at all.")
        
        /* Calculate the number of searcher and the rate of research */
        val nbSearcher = Math.min(searcherBound, queriesToStart.size)
        val searchRate = period * nbSearcher
        var startTime = 0
        
        /* Let's create the duplicate checker */
        val keysSet = queriesToStart.flatten.map(qu => qu._1.keywords).toSet
        val checkerRef = context.actorOf(Props(new TweetDuplicateChecker(queriesToStart.size, keysSet, period)))
        checkerRef.scheduleOnce(searchRate,TimeUnit.SECONDS, Cleanup) /* Schedule the duplicate checker once. It will then schedule itself. */
        actorRefs :+= checkerRef

        /* Create a counter to evenly use the available keys for the Twitter API */
        val twitterKeyCounter = MultiCounter(Math.ceil(queriesToStart.size.toDouble / nbTwitterKeys).toInt)
        
        /* Let's shuffle the list of query and split it for the searchers */
        queriesToStart.flatten.shuffle.split(nbSearcher) foreach { qurs =>
          val searcherRef = context.actorOf(Props(new TweetSearcher(qurs, checkerRef, searchRate, twitterKeyCounter.incr)))
          searcherRef.scheduleOnce(startTime, TimeUnit.SECONDS, Ping) /* Schedule it once. It will then schedule itself. */
          actorRefs :+= searcherRef
          startTime += period
        }
        
        /* We start an instance of the streamer for each of the squares of search*/
        queriesToStart foreach { qurs =>
         val streamerRef = context.actorOf(Props(new TweetStreamer(qurs)))
         streamerRef.scheduleOnce(startTime, TimeUnit.SECONDS, Ping)
         actorRefs :+= streamerRef
         startTime += period
        }
        queriesToStart = Nil /* Reset the queries to start; all are started! */

      case Refused => 
        /* Activated when a TweetSearcher notify the manager that its request has been refused by the API.
         * The manager will then say to all the Searcher to wait a bit before resuming the research, to 
         * avoid cascading refusal from the API. */ 
        actorRefs.tail foreach (_ ! Wait)

      case Stop =>
        assert(!actorRefs.isEmpty, "TweetManager: Cannot stop if nothing is scheduled.")
        actorRefs foreach (_ ! Stop)
        actorRefs = Nil

      case Pause =>
        assert(!actorRefs.isEmpty, "TweetManager: Cannot pause if nothing is scheduled.")
        actorRefs foreach (_! Stop)

      case Resume =>
        assert(!actorRefs.isEmpty, "TweetManager: Cannot resume if nothing is scheduled.")
        actorRefs foreach( _! Resume)

      case _ => sys.error("TweetManager: Wrong message.")
    }
  }

  /* Body of the companion object */

  /* Reference to the singleton Manager */
  val TweetManagerRef = toRef(Props(new TweetManager()))
  
  /* Number of keys available for the Twitter API */
  val nbTwitterKeys = getConfInt("twitter.nbKeys", "No consumer key found in conf.")

  /**
   * @param key		The key set to use. If not specified, is random.
   * @return an OAuth consumer with the keys ready for the Twitter API.
   */
  def consumer(twitterKey: Int = new Random(System.currentTimeMillis).nextInt(nbTwitterKeys)) = {
    /* Access token, saved as Play Configuration Parameters */
    val consumerKey = getConfString(s"twitter.k${twitterKey}.consumerKey", "No consumer key found in conf.")
    val consumerSecret = getConfString(s"twitter.k${twitterKey}.consumerSecret", "No consumer secret found in conf.")
    val accessToken = getConfString(s"twitter.k${twitterKey}.accessToken", "No access token found in conf.")
    val accessTokenSecret = getConfString(s"twitter.k${twitterKey}.accessTokenSecret", "No access token secret found in conf.")

    val consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret)
    consumer.setTokenWithSecret(accessToken, accessTokenSecret)

    consumer
  }
}
