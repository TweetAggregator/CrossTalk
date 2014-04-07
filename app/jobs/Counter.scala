package jobs

import akka.actor.Actor
import akka.actor.ActorRef
import java.io.{PrintWriter, File}

import models._

class Counter(pos: GeoSquare, listener: ActorRef) extends Actor {
  var count = 0L
  val file = new PrintWriter(new File("tweets/counter" + pos.##.toString + ".txt"))

  def increase(n: Int) = {
    count += n
   // listener ! (id, n)
  }

  def get() = count
  /*TODO change this so that report is a default Object*/
  def receive = {
    case ts: Seq[_] => increase(ts.size)
    case ReportCount =>
      listener ! Report(pos, count)
    case t => 
      file.write(t.toString + "\n")
      file.flush()
      increase(1)
  }
}
