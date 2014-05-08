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
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import jobs._
import play.api.libs.json.Json
import akka.actor.{ Actor, ActorRef }
import models._
import jobs.TweetManager._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Future
import clustering.ClustHC
import utils.Enrichments._

@RunWith(classOf[JUnitRunner])
class ClustHCRealTweetSpec extends Specification {

  val nbCol = 12
  val nbRow = 12

  /* Requests are all over the US */
  val queries = List(
    TweetQuery("#GoT" :: "Game of Thrones" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), nbRow, nbCol),
    TweetQuery("Coachella" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), nbRow, nbCol),
    TweetQuery("joffrey lannister" :: "lannister" :: Nil, GeoSquare(-129.4, 20, -79, 50.6), nbRow, nbCol))

  var geoParts = List[ActorRef]()

  "clustering, Manager, GeoPart" should {
    "start new query over all the US and cluster them" in new WithApplication {

      geoParts ++= queries.map(q => toRef(Props(new GeoPartitionner(q.keywords, q.area, q.rows, q.cols))))

      geoParts.foreach(geo => geo ! StartGeo)

      Thread.sleep(10000) /* Wait for all the message to be received */

      TweetManagerRef ! Start
      Thread.sleep(300 * 1000) /* Just sleep for 5 minutes */

      TweetManagerRef ! Stop
      geoParts foreach (_ ! Collect)

      val geoIndices = queries.map(_.computeIndices) zip geoParts

      for (gl <- geoIndices) {
        var leaves: List[LeafCluster] = Nil
        for (g <- gl._1) {
          val fut: Future[Long] = (gl._2 ? TweetsFromSquare(g._2)).mapTo[Long]
          val res: Long = Await.result(fut, Duration(5, "seconds"))

          leaves :+= LeafCluster((g._3, g._4), res, g._2)
        }
        val clusterList = new ClustHC(leaves, nbRow, nbCol).compute

        println("Clustering for: " + gl._1.head._1.head) /* Sorry for that mess... */
        clusterList foreach { set =>
          println()
          set foreach { cluster =>
            println(s"${cluster.topLeft}, ${cluster.bottomRight}, ${cluster.numTweets}")
          }
        }
        println("In JSon:")
        println(clusterList.toJson)
      }
      /* Visual debugging */
    }
  }
}