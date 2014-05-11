package jobs

import akka.actor.Actor
import akka.actor.ActorRef
import java.io.{PrintWriter, File}

import models._

/** Simple counter of tweets (for one request only) */
class Counter(pos: GeoSquare, listener: ActorRef) extends Actor {
  var count = 0L
  val file = new PrintWriter(new File("tweets/counter" + pos.##.toString + ".txt"))

  def receive = {
    case ts: Seq[_] => count + ts.size
    case ReportCount =>
      listener ! Report(pos, count)
      count = 0L /* reset the counter to avoid reporting twice */
    case t => 
      file.write(t.toString + "\n")
      file.flush()
      count += 1
  }
}