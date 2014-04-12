import akka.actor.Actor
import org.specs2.mutable.Specification
import akka.pattern.ask
import akka.util.Timeout
import play.api.test.WithApplication
import utils.AkkaImplicits._
import akka.actor.Props
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration._

class ActorExampleSpec extends Specification {
  
	class Producer extends Actor {
	  def receive = {
	    case "produce" =>
	      Thread.sleep(1000)
	      sender ! "hello"
	      
	  }
	}
	
	"The test actor" should {
	  
	  "receive a message, sleep and return a text" in new WithApplication {
	    
	    val ref = toRef(Props(new Producer()))
	    /*val futureRes = ref ? "produce"
	    println(futureRes.value.get)*/
	  }
	}
}