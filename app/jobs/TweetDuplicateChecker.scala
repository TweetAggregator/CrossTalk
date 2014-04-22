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
import play.api.libs.json.JsNumber
import play.api.libs.concurrent.Execution.Implicits._
import utils.AkkaImplicits._
import scala.language.postfixOps

/**
 * Filter the tweets in order to only keep the non-duplicates.
 * NB: This checker can have some small true negative.
 */
class TweetDuplicateChecker(nbQueries: Int, keywords: Set[List[String]], searcherPeriod: Int) extends Actor {
  
  var running = true /* If true, the actor will be scheduled */
  var scheduled: Option[Cancellable] = None /* cancellable corresponding to the self-schedule of the thread */
  
  /* A map keeping track, for each keyword list used in the research, of the last duplicates */
  var ids: Map[List[String], List[Long]] = keywords.map(k => (k, Nil)) toMap  

  val keep = nbQueries * 100 / keywords.size /* Number of tweets ids to keep per keyword's list */
  val period = searcherPeriod * nbQueries /* Period between each flush of the lists of ids */

  def receive = {

    /* Check for duplicates. If none, send the tweet to the proper listener */
    case (tw @ Tweet(value, query), listener: ActorRef) =>
      val id = (value \ "id").as[JsNumber].value.toLong
      if (!ids(query.keywords).contains(id)) {
        listener ! tw
        ids += (query.keywords -> (ids(query.keywords) :+ id))
      }

    /* Cleanup the list of ids. Some olds ids aren't necessary anymore */
    case Cleanup =>
      if (running) scheduled = Some(self.scheduleOnce(period, TimeUnit.SECONDS, Cleanup))
      ids = ids.keys.map(kwe => (kwe, ids(kwe).takeRight(keep))) toMap

    /* Stop the schedule of the DuplicateChecker */
    case Stop => 
      running = false
      if (scheduled.isDefined) scheduled.get.cancel

    case Resume =>
      running = true
      scheduled = Some(self.scheduleOnce(period, TimeUnit.SECONDS, Cleanup))

    case _ => sys.error("TweetDuplicateChecker: wrong message.")
  }
}
