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
import models.TweetQuery
import models.GeoSquare
import models.Tweet
import play.api.libs.json.Json
import akka.actor.{ Actor, ActorRef }
import models._
import jobs.TweetManager._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Future
import clustering.ClustHC

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
  var squares = List[List[GeoSquare]]()

  "clustering, Manager, GeoPart" should {
    "start new query over all the US and cluster them" in new WithApplication {

      squares ++= queries.map(query => query.subqueries.map(f => f.area))
      geoParts ++= queries.map(q => toRef(Props(new GeoPartitionner(q.keywords, q.area, q.rows, q.cols))))

      geoParts.foreach(geo => geo ! StartGeo)

      Thread.sleep(10000) /* Wait for all the message to be received */

      TweetManagerRef ! Start
      Thread.sleep(100 * 1000) /* Just sleep for 100 seconds */

      for (j <- 0 until squares.size) {
        var leaves: List[LeafCluster] = Nil
        var poss: List[(Int, Int, GeoSquare)] = Nil
        for (i <- 0 until squares(j).size) poss +:= (i % nbCol, i / nbCol, squares(j)(i))
        for (pos <- poss) {

          geoParts(j) ! Collect
          val fut: Future[Long] = (geoParts(j) ? TweetsFromSquare(pos._3)).mapTo[Long]
          val res: Long = Await.result(fut, Duration(5, "seconds"))

          leaves :+= LeafCluster((pos._1, pos._2), res, pos._3)
        }

        val clusterList = new ClustHC(leaves, nbRow, nbCol).compute

        clusterList foreach { set =>
          println()
          set foreach { cluster =>
            println(s"${cluster.topLeft}, ${cluster.bottomRight}, ${cluster.numTweets}")
          }
        }
        
        TweetManagerRef ! Stop
        /* Visual debugging */
      }
    }
  }
}