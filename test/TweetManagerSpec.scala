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
import models._
import jobs.TweetManager._

@RunWith(classOf[JUnitRunner])
class TweetManagerSpec extends Specification {
  var nbReceived = 0

  class Listener extends Actor {
    def receive = {
      case Tweet(value, query) =>
        nbReceived += 1
        print("-")
    }
  }

  /* NB: Those test might fail depending of the network congestion */
  "Tweet Searcher Actor" should {

    "return a list of tweets" in new WithApplication {
      val listener = ActorSystem() actorOf (Props(new Listener))
      val actor = ActorSystem().actorOf(Props(new TweetSearcher(TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1), listener)))
      actor ! Start
      Thread.sleep(30000) /* Just print tweets for 10 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "return a list of tweets and do a callback" in new WithApplication {
      nbReceived = 0
      val listener = ActorSystem() actorOf (Props(new Listener))
      val actor = ActorSystem().actorOf(Props(new TweetSearcher(TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1), listener)))
      actor ! Start
      actor ! Ping
      Thread.sleep(30000) /* Just print tweets for 20 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "launch a query with multiple keywords" in new WithApplication {
      nbReceived = 0
      val listener = ActorSystem() actorOf (Props(new Listener))
      val actor = ActorSystem().actorOf(Props(new TweetSearcher(TweetQuery("Barak Obama" :: "NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1), listener)))
      actor ! Start
      actor ! Ping
      Thread.sleep(20000) /* Just print tweets for 20 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }
  }

  /* NB: Those test might fail depending of the network congestion */
  "Tweet Streamer" should {
    "get some tweets" in new WithApplication {
      nbReceived = 0
      val qur = TweetQuery("a" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      val listener = ActorSystem() actorOf (Props(new Listener))
      val actor = ActorSystem().actorOf(Props(new TweetStreamer(qur, listener)))

      actor ! Start
      Thread.sleep(10000) /* Just print tweets for 20 secs */
      actor ! Ping
      Thread.sleep(1000) /* Just print tweets for 10 secs */
      actor ! Ping
      Thread.sleep(1000) /* Just print tweets for 10 secs */
      actor ! Ping
      Thread.sleep(1000) /* Just print tweets for 10 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "get some tweets from two requests" in new WithApplication {
      nbReceived = 0
      val qur1 = TweetQuery("a" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      val qur2 = TweetQuery("a" :: Nil, GeoSquare(-79, 20, 0, 50.6), 1, 1) /* There must be some tweets contaning "e" ! */
      val listener = ActorSystem() actorOf (Props(new Listener))
      val actor1 = ActorSystem().actorOf(Props(new TweetStreamer(qur1, listener)))
      val actor2 = ActorSystem().actorOf(Props(new TweetStreamer(qur2, listener)))

      actor1 ! Start
      actor2 ! Start
      Thread.sleep(10000) /* Just print tweets for 20 secs */
      actor1 ! Ping
      actor2 ! Ping
      Thread.sleep(1000) /* Just print tweets for 10 secs */
      actor1 ! Ping
      actor2 ! Ping
      Thread.sleep(1000) /* Just print tweets for 10 secs */
      actor1 ! Ping
      actor2 ! Ping
      Thread.sleep(1000) /* Just print tweets for 10 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }
  }

  /* NB: Those test might fail depending of the network congestion */
  "Tweet Manager" should {
    "start queries, and stop them" in new WithApplication {
      nbReceived = 0
      val qurs1 = TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs2 = TweetQuery("NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs3 = TweetQuery("Bloomberg" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val listener = ActorSystem() actorOf (Props(new Listener))

      TweetManagerRef ! AddQueries((qurs1, listener) :: (qurs2, listener) :: (qurs3, listener) :: Nil)
      TweetManagerRef ! Start
      Thread.sleep(40000) /* Just print tweets for 40 secs */
      TweetManagerRef ! Stop
      println("> " + nbReceived)
      nbReceived should be greaterThan (200)
    }
  }
}