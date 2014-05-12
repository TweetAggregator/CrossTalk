import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models._
import utils.Enrichments._

@RunWith(classOf[JUnitRunner])
class ClusteringJsonSpec extends Specification {

  "Pixels" should {
    "be formatted in good way" in new WithApplication {
      val pixels = SuperPixel(10, (10.0, 11.0), 12) :: SuperPixel(20, (103.3, 11.1), 11) :: Nil

      /* Visual Debugging */
      println(pixels.toJson)
    }
  }

  "Clusters" should {
    "be formatted in a good way" in new WithApplication {
      val l1 = LeafCluster((1, 1), 10, GeoSquare(-10.1, -11.1, 21.1, 31.4))
      val l2 = LeafCluster((2, 2), 10, GeoSquare(-110.1, -121.1, 1.1, -2.2))
      val c1 = Set(Cluster(Set(l1)), Cluster(Set(l2)))
      val c2 = Set(Cluster(Set(l1)))

      /* Visual Debugging */
      println((c1 :: c2 :: Nil).toJson)
    }
  }
}

