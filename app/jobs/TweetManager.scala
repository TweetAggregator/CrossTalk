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

/**
 * Manage all the tweets streamer to return from the two APIs, and ensure that the quotas are enforced.
 * Since we want a dynamic update, the receive method can execute various queries :
 * 	1		Stop a specific query (update of the call back schedule)
 *  2		Stop all the querie
 *  3		Start a specific query (update of the call back schedule)
 *  4		Stop all the queries
 *
 *  Note that there should be only one instance of the manager per application.
 */
class TweetManager(listener: TweetListener) extends Actor {
  val system = ActorSystem()
  val searchQueryLimit = 450 * 4 /* per hours, see https://dev.twitter.com/docs/rate-limiting/1.1/limits */
  private def toSeconds(perHour: Int) = (60 * 60) / perHour

  var searchRate = 0
  var searchRunnings: List[Cancellable] = Nil /* A list of running and cancellable queries */

  def receive = { /* TODO: cover cases for the Streaming API */
    case StartAll(queries) =>
      val subqueries = queries.flatMap(_.subqueries)
      searchRate = searchQueryLimit / subqueries.size
      subqueries foreach { query =>
        val searcherRef = system.actorOf(Props(new TweetSearcher(query, listener)))
        searcherRef ! "start"
        searchRunnings :+= system.scheduler.schedule(
          Duration.create(toSeconds(searchRate), TimeUnit.SECONDS),
          Duration.create(toSeconds(searchRate), TimeUnit.SECONDS),
          searcherRef, "callback")
      }

    case StopAll =>
      searchRunnings foreach (_.cancel)
      searchRunnings = Nil

    case Start(query) => /* TODO */
    case Stop(query) => /* TODO */
    case _ => sys.error("Wrong message sent to the TweetManager")
  }
}
object TweetManager {

  type TweetListener = ActorRef /* The listener is another actor */

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