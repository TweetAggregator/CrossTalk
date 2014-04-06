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
    val query1 = TweetQuery("test" :: Nil, GeoSquare(-2,-3,2,3), 4,3)
    val query2 = TweetQuery("Obama" :: Nil, GeoSquare(-129.4, 20.0, -79, 50.6), 2, 2)
    
    "have proper geographic subsquares (T1)" in new WithApplication {
      val subqueries = query1.subqueries
      subqueries.size should equalTo (4*3)
      subqueries.tail.head.area.lat1 should equalTo (-1)
    }
    
    "have proper geographic subsquares (T2)" in new WithApplication {
      val subqueries = query2.subqueries
      subqueries.size should equalTo (2*2)
    }
    
    "return valid radius and center" in new WithApplication {
      query1.area.center should equalTo ((0.0,0.0))
      query1.area.radius should equalTo (401.31168373453505)
    }
  }
}
