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

@RunWith(classOf[JUnitRunner])
class TweetStreamerSpec extends Specification {
    var nbReceived = 0
    
  class listener extends Actor {
    def receive = {
      case Tweet(value, geo) =>  
        println(value \ "text")
        nbReceived += 1
    }
  }

  "Tweet Searcher Actor" should {
    
    "return a list of tweets " in new WithApplication {
      val actor = ActorSystem().actorOf(Props(new TweetSearcher))
      val listenerAct = ActorSystem().actorOf(Props(new listener))
      actor ! (TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 50.6, -79, 20), 1, 1), listenerAct, 3000)
      Thread.sleep(10000) /* Just print tweets for 100 secs */
      actor ! "terminate"
      println("Nb tweets received: " + nbReceived)
      
    }
  }
}
