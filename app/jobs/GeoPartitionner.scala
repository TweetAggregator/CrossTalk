package jobs

import akka.actor.Actor
import models.GeoSquare
import models._

class GeoPartitionner extends Actor {

  def receive = {
   case _ => println("So happy you could die")
   /* case GeoSend(res) => 
      println("res "+res.map(_._2).reduce((a, b) => a + b))
      println("Winner is "+ res.maxBy(_._2))
  */}
}
