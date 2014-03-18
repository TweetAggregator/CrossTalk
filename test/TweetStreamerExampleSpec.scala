import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import jobs._

@RunWith(classOf[JUnitRunner])
class TweetStreamerExampleSpec extends Specification {

  "Tweet Streamer Example Actor" should {
    "Launch a list of tweets" in new WithApplication {
      val actor = ActorSystem().actorOf(Props(new TweetStreamExampleActor()))
      actor ! Array("Crimea", "Putin")

      Thread.sleep(100000) /* Just print tweets for 100 secs */
    }
  }
}