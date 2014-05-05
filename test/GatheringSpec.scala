import org.specs2.mutable._
import org.specs2.runner._

import controllers._
import play.api._
import play.api.mvc._
import play.api.test._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache

object GatheringControllerSpec extends Specification {

  class TestController() extends Controller with GatheringController

  "Gathering controller" should {
    "read data from the cache" in new WithApplication {
      val controller = new TestController()

      Cache.set("squares", List((-129.4, 20.0, -79.0, 50.6)))
      Cache.set("fromTimoToJorisAndLewis",
                List(("Obama", List[String]()),
                     ("Beer", List("biere", "pression"))))

      controller.start() must not beNull

      Thread.sleep(2000)
    }
  }

  //TODO: test with too many keywords
}
