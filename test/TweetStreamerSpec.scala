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
import models.Tweet
import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class TweetStreamerSpec extends Specification {

  val listener = new TweetListener { 
    def act(tweet: Tweet) = println(tweet.value \ "text") 
    }

  "Tweet Searcher Actor" should {
    
    "return a list of tweets " in new WithApplication {
      val actor = ActorSystem().actorOf(Props(new TweetSearcher()))
      actor ! (TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1), listener, 3000)
      Thread.sleep(10000) /* Just print tweets for 100 secs */
      actor ! "terminate"
      
    }
  }
}
