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
import utils.AkkaImplicits._

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

  /** A list of running and cancellable Searcher, along with their reference actors */
  var runningSearches: Map[TweetQuery, (Cancellable, ActorRef)] = Map()

  def receive = {

    case StartAll(queries) =>
      val searchRate = if (queries.size > 0) searchQueryLimit / queries.size else 0
      queries foreach { qur =>
        val searcherRef = toRef(Props(new TweetSearcher(qur._1, qur._2)))
        searcherRef ! "start"
        scheduleSearcher(qur._1, qur._2, searchRate)
      }

    case StopAll =>
      runningSearches.values foreach (_._1.cancel)
      runningSearches = Map()

    case Stop(query) =>
      runningSearches get (query) match {
        case Some(qur) =>
          runningSearches.values foreach (_._1.cancel)
          runningSearches -= query
          val refs = runningSearches map (qur => (qur._1, qur._2._2))
          runningSearches = Map()
          val searchRate = if (refs.size > 0) searchQueryLimit / refs.size else 0
          refs foreach (qur => scheduleSearcher(qur._1, qur._2, searchRate))
        case None => sys.error("Want to stop a query never launched.")
      }

    case Start(query, listener) =>
      runningSearches.values foreach (_._1.cancel)
      val refs = runningSearches map (qur => (qur._1, qur._2._2))
      runningSearches = Map()
      val searchRate = searchQueryLimit / (refs.size + 1)
      refs foreach (qur => scheduleSearcher(qur._1, qur._2, searchRate))
      val searcherRef = toRef(Props(new TweetSearcher(query, listener)))
      searcherRef ! "start"
      scheduleSearcher(query, searcherRef, searchRate)

    case _ => sys.error("Wrong message sent to the TweetManager")
  }

  /**
   * Reschedule a searcher 'searcherRef' based on the query 'query' with the specified search rate.
   */
  def scheduleSearcher(query: TweetQuery, searcherRef: ActorRef, searchRate: Int) {
    runningSearches += (query -> (
      searcherRef.schedule(toSeconds(searchRate), toSeconds(searchRate), TimeUnit.SECONDS, "callback"),
      searcherRef))
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
