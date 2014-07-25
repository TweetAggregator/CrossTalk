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
  def appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test"))

  class Listener extends Actor {
    def receive = {
      case Tweet(value, query) =>
        nbReceived += 1
        print("-")
    }
  }
  
  /* NB: Those test might fail depending of the network congestion */
  "Tweet Searcher Actor" should {

    "return a list of tweets" in new WithApplication(appWithMemoryDatabase) {
      val query = TweetQuery(FirstGroup, "Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val manager = Akka.system.actorOf(Props(new TweetManager(1, new SQLDataStore)))
      val listener = Akka.system.actorOf(Props(new Listener))
      val checker = Akka.system.actorOf(Props(new TweetDuplicateChecker(1, Set(query.keywords), 2)))
      val actor = Akka.system.actorOf(Props(new TweetSearcher((query, listener) :: Nil, checker, 30, 0, manager)))

      actor ! Ping
      Thread.sleep(30000) /* Just print tweets for 30 secs */
      actor ! Stop
      Thread.sleep(1000)
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "return a list of tweets and do a callback" in new WithApplication {
      nbReceived = 0
      val query = TweetQuery(FirstGroup, "Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val manager = Akka.system.actorOf(Props(new TweetManager(1, new SQLDataStore)))
      val checker = Akka.system.actorOf(Props(new TweetDuplicateChecker(1, Set(query.keywords), 2)))
      val listener = Akka.system.actorOf(Props(new Listener))
      val actor = Akka.system.actorOf(Props(new TweetSearcher((query, listener) :: Nil, checker, 10, 0, manager)))

      actor ! Ping
      Thread.sleep(30000) /* Just print tweets for 30 secs */
      actor ! Stop
      Thread.sleep(1000)
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }

    "launch a query with multiple keywords" in new WithApplication {
      nbReceived = 0
      val query = TweetQuery(FirstGroup, "Barak Obama" :: "NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val manager = Akka.system.actorOf(Props(new TweetManager(1, new SQLDataStore)))
      val checker = Akka.system.actorOf(Props(new TweetDuplicateChecker(1, Set(query.keywords), 2)))
      val listener = Akka.system.actorOf(Props(new Listener))
      val actor = Akka.system.actorOf(Props(new TweetSearcher((query, listener) :: Nil, checker, 20, 0, manager)))

      actor ! Ping
      Thread.sleep(20000) /* Just print tweets for 20 secs */
      actor ! Stop
      Thread.sleep(1000)
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }
  }
  
  /* NB: Those test might fail depending of the network congestion */
  "Tweet Streamer" should {
    "get some tweets" in new WithApplication {
      nbReceived = 0
      val qur = TweetQuery(FirstGroup, "NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      val qur2 = TweetQuery(FirstGroup, "Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qur3 = TweetQuery(FirstGroup, "hey" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      var listQuery: List[(TweetQuery, ActorRef)] = Nil
      val listener = Akka.system.actorOf(Props(new Listener))

      listQuery :+= (qur, listener)
      listQuery :+= (qur2, listener)
      listQuery :+= (qur3, listener)
      val actor = Akka.system.actorOf(Props(new TweetStreamer(listQuery)))

      actor ! Start
      Thread.sleep(20000) /* Just print tweets for 20 secs */
      actor ! Stop
      Thread.sleep(1000)
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
      actor ! Stop
    }

    "get some tweets from two requests" in new WithApplication {
      nbReceived = 0
      val qur1 = TweetQuery(FirstGroup, "morning" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      val qur2 = TweetQuery(FirstGroup, "night" :: Nil, GeoSquare(-79, 20, 0, 50.6), 1, 1) /* There must be some tweets contaning "e" ! */
      val listener = Akka.system.actorOf(Props(new Listener))

      var listQuery2: List[(TweetQuery, ActorRef)] = Nil
      var listQuery3: List[(TweetQuery, ActorRef)] = Nil
      listQuery2 :+= (qur1, listener)
      listQuery3 :+= (qur2, listener)
      val actor1 = Akka.system.actorOf(Props(new TweetStreamer(listQuery2)))
      val actor2 = Akka.system.actorOf(Props(new TweetStreamer(listQuery3)))

      actor1 ! Start
      actor2 ! Start
      Thread.sleep(40000) /* Just print tweets for 40 secs */
      println("> " + nbReceived)
      actor1 ! Stop
      actor2 ! Stop
      Thread.sleep(1000)
      nbReceived should be greaterThan (0)
    }
  }
  
  "Tweet Tester" should {
    "get some tweets" in new WithApplication {
      nbReceived = 0
      val qur = TweetQuery(FirstGroup, "NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      val qur2 = TweetQuery(FirstGroup, "Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qur3 = TweetQuery(FirstGroup, "hey" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      var listQuery: List[(TweetQuery, ActorRef)] = Nil
      val listener = Akka.system.actorOf(Props(new Listener))

      listQuery :+= (qur, listener)
      listQuery :+= (qur2, listener)
      listQuery :+= (qur3, listener)
      val actor = Akka.system.actorOf(Props(new TweetTester(listQuery)))

      actor ! Start
      Thread.sleep(20000) /* Just print tweets for 20 secs */
      actor ! Stop
      Thread.sleep(1000)
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
      actor ! Stop
    }

    "get some tweets from two requests" in new WithApplication {
      nbReceived = 0
      val qur1 = TweetQuery(FirstGroup, "morning" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1) /* There must be some tweets contaning "a" ! */
      val qur2 = TweetQuery(FirstGroup, "night" :: Nil, GeoSquare(-79, 20, 0, 50.6), 1, 1) /* There must be some tweets contaning "e" ! */
      val listener = Akka.system.actorOf(Props(new Listener))

      var listQuery2: List[(TweetQuery, ActorRef)] = Nil
      var listQuery3: List[(TweetQuery, ActorRef)] = Nil
      listQuery2 :+= (qur1, listener)
      listQuery3 :+= (qur2, listener)
      val actor1 = Akka.system.actorOf(Props(new TweetTester(listQuery2)))
      val actor2 = Akka.system.actorOf(Props(new TweetTester(listQuery3)))

      actor1 ! Start
      actor2 ! Start
      Thread.sleep(40000) /* Just print tweets for 40 secs */
      println("> " + nbReceived)
      actor1 ! Stop
      actor2 ! Stop
      Thread.sleep(1000)
      nbReceived should be greaterThan (0)
    }
  }
  /* NB: Those test might fail depending of the network congestion */
  "Tweet Manager" should {
    "start queries, and stop them" in new WithApplication {
      nbReceived = 0
      val qurs1 = TweetQuery(FirstGroup, "Obama" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs2 = TweetQuery(FirstGroup, "Ukraine" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs3 = TweetQuery(FirstGroup, "Bloomberg" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs4 = TweetQuery(FirstGroup, "Putin" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)
      val qurs5 = TweetQuery(FirstGroup, "NSA" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 1, 1)

      val listener = Akka.system.actorOf(Props(new Listener))
      val manager = Akka.system.actorOf(Props(new TweetManager(1, new SQLDataStore())))

      manager ! AddQueries((qurs1, listener) :: (qurs2, listener) :: (qurs3, listener) :: (qurs4, listener) :: (qurs5, listener) :: Nil)
      manager ! Start
      Thread.sleep(40000) /* Just print tweets for 40 secs */
      /*manager ! Pause
      manager ! Resume*/
      Thread.sleep(20000)
      manager ! Stop
      Thread.sleep(5000) /* Just wait for 20 secs, nothing more should be received. */
      println("> " + nbReceived)
      nbReceived should be greaterThan (0)
    }
  }
}
