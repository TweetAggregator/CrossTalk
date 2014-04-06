package jobs

import akka.actor.Actor
import akka.actor.ActorRef

import models._

class Counter(listener: ActorRef) extends Actor {
  var count = 0L

  def increase(n: Int) = {
    count += n
    listener ! n
  }

  def get() = count

  def receive = {
    case ts: Seq[_] => increase(ts.size)
    case _ => 
      increase(1)
  }
}
