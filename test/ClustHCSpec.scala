import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import jobs.DummyActor
import models._
import scala.util.Random
import clustering.ClustHC

@RunWith(classOf[JUnitRunner])
class ClustHCSpec extends Specification {
	
  "Clustering HC" should {
    "cluster something" in new WithApplication {
      val random = Random
      
      val rows = 10
      val cols = 10
      /* Dummy geoSquare : isn't needed for the test */
      val leaves = (0 until (rows * cols)) map (i => LeafCluster((i / rows, i % rows), random.nextInt(100), GeoSquare(0,0,1,1))) toList
      
      val clustHC = new ClustHC(leaves, rows, cols)
      val clusterList = clustHC.compute
      clusterList foreach { set =>
        println()
        set foreach { cluster =>
          println(s"${cluster.topLeft}, ${cluster.bottomRight}, ${cluster.numTweets}")
        }
      }
      /* Visual debugging */
    }
  }
}