package jobs

import akka.actor.Actor
import akka.actor.ActorRef

import models._

class Counter(id: (Int, Int), listener: ActorRef) extends Actor {
  var count = 0L

  def increase(n: Int) = {
    count += n
    listener ! (id, n)
  }

  def get() = count

  def receive = {
    case ts: Seq[_] => increase(ts.size)
    case _ => 
      increase(1)
  }
}
