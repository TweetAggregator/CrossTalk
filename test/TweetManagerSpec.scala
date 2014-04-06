import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import jobs._
import models.TweetQuery
import models.GeoSquare
import models.Tweet
import play.api.libs.json.Json
import akka.actor.Actor
import models.StartAll
import models.StopAll
import models.Replace

@RunWith(classOf[JUnitRunner])
class TweetManagerSpec extends Specification {
  var nbReceived = 0

  class listener extends Actor {
    def receive = {
      case Tweet(value, geo) =>
        nbReceived += 1
        print("-")
    }
  }

  /* NB: Those test might fail depending of the network congestion */
  "Tweet Searcher Actor" should {

    "return a list of tweets" in new WithApplication {
      val listener = ActorSystem() actorOf (Props(new listener))
      val actor = ActorSystem().actorOf(Props(new TweetSearcher(TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1), listener)))
      actor ! "start"
      Thread.sleep(20000) /* Just print tweets for 10 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan(0)
    }

    "return a list of tweets and do a callback" in new WithApplication {
      nbReceived = 0
      val listener = ActorSystem() actorOf (Props(new listener))
      val actor = ActorSystem().actorOf(Props(new TweetSearcher(TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1), listener)))
      actor ! "start"
      actor ! "callback"
      Thread.sleep(20000) /* Just print tweets for 20 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "launch a query with multiple keywords" in new WithApplication {
      nbReceived = 0
      val listener = ActorSystem() actorOf (Props(new listener))
      val actor = ActorSystem().actorOf(Props(new TweetSearcher(TweetQuery("Barak Obama" :: "NSA" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1), listener)))
      actor ! "start"
      actor ! "callback"
      Thread.sleep(20000) /* Just print tweets for 20 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }
  }

  /* NB: Those test might fail depending of the network congestion */
  "Tweet Manager" should {
    "start queries, and stop them" in new WithApplication {
      nbReceived = 0
      val qurs1 = TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1)
      val qurs2 = TweetQuery("NSA" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1)
      val qurs3 = TweetQuery("Bloomberg" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1)

      val manager = ActorSystem().actorOf(Props(new TweetManager))
      val listener = ActorSystem() actorOf (Props(new listener))

      manager ! StartAll((qurs1, listener) :: (qurs2, listener) :: (qurs3, listener) :: Nil)
      Thread.sleep(30000) /* Just print tweets for 30 secs */
      manager ! StopAll
      println("> " + nbReceived)
      nbReceived should be greaterThan (200)
    }

    "start a query, replace it by another, then stop it" in new WithApplication {
      nbReceived = 0
      val qurs1 = TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1)
      val qurs2 = TweetQuery("NSA" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1)

      val manager = ActorSystem().actorOf(Props(new TweetManager))
      val listener = ActorSystem() actorOf (Props(new listener))

      manager ! StartAll((qurs1, listener) :: Nil)
      Thread.sleep(20000) /* Wait 20 seconds for the first tweet to be received */
      manager ! Replace(qurs1, (qurs2, listener) :: Nil)
      Thread.sleep(20000) /* Wait 20 seconds for the next tweets to be received */
      manager ! StopAll
      println("> " + nbReceived)
      nbReceived should be greaterThan (100)
    }

  }
}
