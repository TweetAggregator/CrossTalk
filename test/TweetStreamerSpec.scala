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
import models.TweetListener

@RunWith(classOf[JUnitRunner])
class TweetStreamerSpec extends Specification {

  "Tweet Searcher Actor" should {
    "Return a list of tweets " in new WithApplication {
      val actor = ActorSystem().actorOf(Props(new TweetSearcher()))
      val listener = new TweetListener { def receive = println("received") }
      actor ! (TweetQuery("Obama" :: Nil, GeoSquare(-129.4,50.6,-79,20), 1,1),listener, 0)
      Thread.sleep(10000) /* Just print tweets for 100 secs */
    }
  }
}
