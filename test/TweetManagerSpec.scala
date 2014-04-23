import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.Props
import jobs._
import models.TweetQuery
import models.GeoSquare
import models.Tweet
import play.api.libs.json.Json
import akka.actor.Actor
import models._
import jobs.TweetManager._
import play.libs.Akka
import akka.actor.ActorRef

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
      val query = TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val listener = Akka.system.actorOf(Props(new Listener))
      val checker = Akka.system.actorOf(Props(new TweetDuplicateChecker(1, Set(query.keywords), 2)))
      val actor = Akka.system.actorOf(Props(new TweetSearcher((query, listener) :: Nil,checker, 30, 0)))

      actor ! Ping
      Thread.sleep(30000) /* Just print tweets for 10 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "return a list of tweets and do a callback" in new WithApplication {
      nbReceived = 0
      val query = TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val checker = Akka.system.actorOf(Props(new TweetDuplicateChecker(1, Set(query.keywords),2)))
      val listener = Akka.system.actorOf(Props(new Listener))
      val actor = Akka.system.actorOf(Props(new TweetSearcher((query, listener) :: Nil,checker, 10, 0)))

      actor ! Ping
      Thread.sleep(30000) /* Just print tweets for 20 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "launch a query with multiple keywords" in new WithApplication {
      nbReceived = 0
      val query = TweetQuery("Barak Obama" :: "NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val checker = Akka.system.actorOf(Props(new TweetDuplicateChecker(1, Set(query.keywords),2)))
      val listener = Akka.system.actorOf(Props(new Listener))
      val actor = Akka.system.actorOf(Props(new TweetSearcher((query, listener) :: Nil,checker, 20, 0)))

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
      val qur = TweetQuery("hey" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      var listQuery: List[(TweetQuery, ActorRef)] = Nil
      val listener = Akka.system.actorOf(Props(new Listener))
      
      listQuery :+= (qur, listener)
      val actor = Akka.system.actorOf(Props(new TweetStreamer(listQuery)))

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
      actor ! Stop
    }

    "get some tweets from two requests" in new WithApplication {
      nbReceived = 0
      val qur1 = TweetQuery("morning" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      val qur2 = TweetQuery("night" :: Nil, GeoSquare(-79, 20, 0, 50.6), 1, 1) /* There must be some tweets contaning "e" ! */
      val listener = Akka.system.actorOf(Props(new Listener))
      
      var listQuery2: List[(TweetQuery, ActorRef)] = Nil
      var listQuery3: List[(TweetQuery, ActorRef)] = Nil
      listQuery2 :+= (qur1, listener)
      listQuery3 :+= (qur2, listener)
      val actor1 = Akka.system.actorOf(Props(new TweetStreamer(listQuery2)))
      val actor2 = Akka.system.actorOf(Props(new TweetStreamer(listQuery3)))

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
      val qurs2 = TweetQuery("Ukraine" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs3 = TweetQuery("Bloomberg" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs4 = TweetQuery("Putin" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs5 = TweetQuery("NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs6 = TweetQuery("the" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val listener =Akka.system.actorOf(Props(new Listener))

      TweetManagerRef ! AddQueries((qurs1, listener) :: (qurs2, listener) :: (qurs3, listener) :: (qurs4, listener) :: (qurs5, listener) :: Nil)
      TweetManagerRef ! Start
      Thread.sleep(40000) /* Just print tweets for 40 secs */
      /*TweetManagerRef ! Pause
      TweetManagerRef ! Resume*/
      Thread.sleep(20000)
      TweetManagerRef ! Stop
      Thread.sleep(20000) /* Just print tweets for 40 secs */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }
  }
}
