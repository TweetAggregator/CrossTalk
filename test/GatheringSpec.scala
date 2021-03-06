import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.mock._
import org.specs2.mock.Mockito

import akka.actor.Props
import akka.actor.Actor
import controllers._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache

import jobs._
import models._
import utils.AkkaImplicits._
import TweetManager._

object GatheringControllerSpec extends Specification with Mockito {

  // Providing fake receive methods for the actors used avoids tests querying Twitter
  class FakeGatheringActors(geoPartRecv: Actor => PartialFunction[Any,Unit], tweetManagerRecv: Actor => PartialFunction[Any,Unit]) extends GatheringActors {
    def geoPartitioner(keywords: List[String], square: GeoSquare, row: Int, col: Int) = {
      val spiedGeoPart = spy(new GeoPartitionner(keywords, square, row, col))
      spiedGeoPart.receive returns (geoPartRecv(spiedGeoPart))
      spiedGeoPart
    }
    def tweetManager = {
      val spiedTweetManager = spy(new TweetManager())
      spiedTweetManager.receive returns (tweetManagerRecv(spiedTweetManager))
      spiedTweetManager
    }
    def tweetManagerRef = toRef(Props(tweetManager))
  }

  class TestController(geoPart: Actor => PartialFunction[Any,Unit], tweetManager: Actor => PartialFunction[Any,Unit]) extends Controller with GatheringController {
    //val actors = new FakeGatheringActors(geoPart, tweetManager)
    val actors =  new GatheringActorsImpl
  }

  "Gathering controller" should {
    "return OK on well-formed data" in new WithApplication {
      val geoPartRecv: Actor => PartialFunction[Any, Unit] = a => {
        case StartGeo => a.sender ! Done
        case TweetsFromSquare(_) => a.sender ! 14
      }
      val tweetManagerRecv: Actor => PartialFunction[Any, Unit] = a => {
        case Start => ()
      }
      val controller = new TestController(geoPartRecv, tweetManagerRecv)

      Cache.set("coordinates", List((-129.4, 20.0, -79.0, 50.6)))
      Cache.set("keywords",
                List(("Obama", List[String]()),
                     ("Beer", List("biere", "pression"))))
      Cache.set("focussed", (-109.4, 40.0, -79.0, 50.6))

      controller.start() must be (controller.Ok)

      Thread.sleep(20000)

      controller.computeDisplayData must be (controller.Ok)

      Thread.sleep(8000)
    }
  }

  //TODO: test timeouts
  //TODO: test with too many keywords
  //TODO: test double start
}
