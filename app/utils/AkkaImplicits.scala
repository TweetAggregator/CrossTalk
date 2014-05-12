package utils

import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.Actor
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import play.libs.Akka
import scala.concurrent.duration._
import akka.util.Timeout

/**
 * Implicit class for Akka functionality. From props, it can schedule 
 * automatically an actor, and set up a periodic call.
 */
object AkkaImplicits {
  import Enrichments._
  
  /** Schedule an Actor Prop and return its reference */
  implicit def toRef(a: Props): ActorRef = Akka.system.actorOf(a)

  /** implicit timeout for the actor system: we always wait, since it's a requirement for every action to succeed here. */ 
  implicit def defaultTimeout : Timeout = Timeout(defaultDuration)
  implicit def defaultDuration = getConfInt("akka.defaultTimeout", "No default timeout specified for akka messages.") minutes
  
  implicit class Scheduler(a: ActorRef) {

    /** Schedule periodically a message sent to the specified actorRef */
    def schedule(initialDelay: Int, interval: Int, timeType: TimeUnit, message: Any) = Akka.system.scheduler.schedule(
      Duration.create(initialDelay, timeType),
      Duration.create(interval, timeType),
      a, message)

    /** Schedule once, in the future, a message sent to the specified actorRef */
    def scheduleOnce(delay: Int, timeType: TimeUnit, message: Any) = Akka.system.scheduler.scheduleOnce(
      Duration.create(delay, timeType),
      a, message)
  }
}