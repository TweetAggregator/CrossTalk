import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import jobs.DummyActor

@RunWith(classOf[JUnitRunner])
class DummySpec extends Specification {

  "Dummy Actor" should {
    "Print Hello world" in new WithApplication {
      val dummyActor = ActorSystem().actorOf(Props(new DummyActor()))
      dummyActor ! "Hello World !"
    }
  }
}