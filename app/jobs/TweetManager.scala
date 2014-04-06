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
import TweetManager._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._

/* TODO: cover cases for the Streaming API */
/**
 * Manage all the tweets streamer to return from the two APIs, and ensure that the quotas are enforced.
 * Since we want a dynamic update, the receive method can execute various queries :
 *  1		Start multiple queries
 *  2		Stop all the queries
 *  3		Stop a specific query and launch subqueries in order to refine the research
 *
 *  Note that there should be only one instance of the manager per application.
 */
class TweetManager extends Actor {

  val searchQueryLimit = 180 * 4 /* per hours, see https://dev.twitter.com/docs/rate-limiting/1.1/limits */
  private def toSeconds(perHour: Int) = (60 * 60) / perHour

  /** A list of running and cancellable Searcher, along with their update rates */
  var runningSearches: Map[TweetQuery, (Cancellable, Int)] = Map()

  def receive = {

    case StartAll(queries) =>
      assert(queries.size > 0, "Cannot start no queries at all.")
      val searchRate = searchQueryLimit / queries.size
      queries foreach { qur => scheduleSearcher(qur._1, qur._2, searchRate) }

    case StopAll =>
      runningSearches.values foreach (_._1.cancel)
      runningSearches = Map()

    case Replace(origin, queries) =>
      assert(queries.size > 0, "Cannot replace a query by none.")
      runningSearches get (origin) match {
        case Some(qur) =>
          qur._1.cancel
          runningSearches -= origin
          val newSearchRate = qur._2 / queries.size
          queries foreach (qur => scheduleSearcher(qur._1, qur._2, newSearchRate))

        case None => sys.error("Want to stop a query never launched.")
      }

    case _ => sys.error("Wrong message sent to the TweetManager")
  }

  /**
   * start the query in parameters, with the corresponding TweetListener, at the specified search
   * rate. The list of running searches is updated.
   */
  def scheduleSearcher(query: TweetQuery, listener: TweetListener, searchRate: Int) {
    val searcherRef = ActorSystem().actorOf(Props(new TweetSearcher(query, listener)))
    /*val streamerRef = ActorSystem().actorOf(Props(new TweetStreamer(query, listener)))*/

    searcherRef ! "start"
    /*streamerRef ! "start"*/ 

    runningSearches += (query -> (ActorSystem().scheduler.schedule(
      Duration.create(toSeconds(searchRate), TimeUnit.SECONDS),
      Duration.create(toSeconds(searchRate), TimeUnit.SECONDS),
      searcherRef, "callback"), searchRate))
     /*runningSearches += (query -> (ActorSystem().scheduler.schedule(
      Duration.create(toSeconds(searchRate), TimeUnit.SECONDS),
      Duration.create(toSeconds(searchRate), TimeUnit.SECONDS),
      streamerRef, "callback"), searchRate))*/
  }
}
object TweetManager {

  type TweetListener = ActorRef /* The listener is another actor */

  /**
   * Return an OAuth consumer with the keys ready for the Twitter API.
   */
  def getConsumer = {
    /* Access token, saved as Play Configuration Parameters */
    val consumerKey = Play.current.configuration.getString("twitter.consumerKey").getOrElse(sys.error("No consumer key found in conf."))
    val consumerSecret = Play.current.configuration.getString("twitter.consumerSecret").getOrElse(sys.error("No consumer secret found in conf."))
    val accessToken = Play.current.configuration.getString("twitter.accessToken").getOrElse(sys.error("No access token found in conf."))
    val accessTokenSecret = Play.current.configuration.getString("twitter.accessTokenSecret").getOrElse(sys.error("No access token secret found in conf."))

    val consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret)
    consumer.setTokenWithSecret(accessToken, accessTokenSecret)

    consumer
  }
}
