import akka.actor.Props
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Actor
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._

/* TODO: improve */

/**
 * Implicit class for akkas functionality.
 * From props, it can schedule automatically an actor, and set up a periodic
 * call.
 */
object AkkaImplicits {
  /** Schedule an Actor Prop and return its reference */
  implicit def toRef(a: Props): ActorRef = ActorSystem().actorOf(a)

  implicit class Scheduler(a: ActorRef) {

    /** Schedule periodically a message setn to the specified actorRef */
    def schedule(initialDelay: Int, interval: Int, timeType: TimeUnit, message: Any) = ActorSystem().scheduler.schedule(
      Duration.create(initialDelay, timeType),
      Duration.create(interval, timeType),
      a, message)

    /** Schedule once, in the future, a message sent to an actor */
    def scheduleOnce(delay: Int, timeType: TimeUnit, message: Any) = ActorSystem().scheduler.scheduleOnce(
      Duration.create(delay, timeType),
      a, message)
  }
}