import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.TweetQuery
import models.GeoSquare

@RunWith(classOf[JUnitRunner])
class TweetQuerySpec extends Specification {
  "A TweetQuery" should {
    val query = TweetQuery("test" :: Nil, GeoSquare(-2,-3,2,3), 4,3)
    
    "have proper geographic subsquares" in new WithApplication {
      println(query)
      val subqueries = query.subqueries
      assert(subqueries.size == 4*3)
      assert(subqueries.tail.head.area.lat1 == -1)
    }
    
    "return valid radius and center" in new WithApplication {
      assert (query.area.center == (0,0))
      println(query.area.radius == 802.6233674690701)
    }
  }
}