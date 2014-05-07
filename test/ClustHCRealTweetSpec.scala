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
import akka.actor.ActorRef
import play.api.libs.json.JsNumber
import utils.AkkaImplicits._
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(classOf[JUnitRunner])
class ClustHCRealTweetSpec extends Specification {
  
  /* Requests are all over the US */
  val queries = List(
    TweetQuery("#GoT" :: "Game of Thrones" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 12, 12),
    TweetQuery("Coachella" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 12, 12),
    TweetQuery("joffrey lannister" :: "lannister" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), 12, 12))

  var geoParts = List[ActorRef]()
  var squares = List[GeoSquare]()

  "clustering, Manager, GeoPart" should {
    "start new query over all the US and cluster them" in new WithApplication {
      
      squares ++= queries.flatMap(query => query.subqueries).map(f => f.area)
      geoParts ++= queries.map(q => toRef(Props(new GeoPartitionner(q.keywords, q.area, q.rows, q.cols))))
      
      geoParts.foreach(geo => geo ! StartGeo)
      
      Thread.sleep(10000) /* Wait for all the message to be received */
      
      TweetManagerRef ! Start
      Thread.sleep(100*1000) /* Just sleep for 100 seconds */
      

    }
  }
}