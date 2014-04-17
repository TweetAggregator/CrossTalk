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
  class TweetDuplicateChecker(toKeep: Int, keywords: Set[List[String]]) extends Actor {
        /* A list keeping track of the last duplicates */
    var ids: Map[List[String], List[Long]] = keywords.map(k => (k,Nil)) toMap

    def receive = {

      /* Check for duplicates. If none, send the tweet to the proper listener */
      case (tw @ Tweet(value, query), listener: ActorRef) =>
        val id = (value \ "id").as[JsNumber].value.toLong
        if(!ids(query.keywords).contains(id)) {
                listener ! tw
                ids+= (query.keywords -> (ids(query.keywords) :+ id))
        }

      /* Cleanup the list of ids. Some olds ids aren't necessary anymore */
      case Cleanup =>
        ids = ids.keys.map(kwe => (kwe, ids(kwe).takeRight(toKeep))) toMap

      case _ => sys.error("Wrong message sent to the DuplicateChecker")
    }
  }
