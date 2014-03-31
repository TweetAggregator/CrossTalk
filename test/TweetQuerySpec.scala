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
      val subqueries = query.subqueries
      subqueries.size should equalTo (4*3)
      subqueries.tail.head.area.lat1 should equalTo (-1)
    }
    
    "return valid radius and center" in new WithApplication {
      query.area.center should equalTo ((0.0,0.0))
      query.area.radius should equalTo (401.31168373453505)
    }
  }
}