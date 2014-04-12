import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import jobs._
import models.TweetQuery
import models.GeoSquare
import models.Tweet
import play.api.libs.json.Json
import akka.actor.{Actor, ActorRef}
import models._
import scala.concurrent.duration.Duration
import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class HierarchySpec extends Specification {
 var track = 0L
 val defaultRow = 4
 val defaultCol = 5
 class notify extends Actor {
  def receive = {
    case Report(id, count) =>
      track += count
  }
 }

  implicit def toRef(a: Props): ActorRef = {
    ActorSystem().actorOf(a)
  }
  "Hierarchy Test" should {
    
    "Multiple Queries" in new WithApplication {
      /*GeoPartitionner test*/
      val geoPart: ActorRef = Props(new GeoPartitionner("Obama"::Nil, GeoSquare(-129.4, 20.0, -79, 50.6), defaultRow, defaultCol))
            
      geoPart ! StartGeo
      Thread.sleep(20000)
      geoPart ! Collect
      Thread.sleep(1000)
      val totalFuture = geoPart ? TotalTweets
      val res = Await.result(totalFuture, Duration(5, "seconds"))
      println(s"Total from future: $res")
      geoPart ! Winner
  
     }
  }
}
