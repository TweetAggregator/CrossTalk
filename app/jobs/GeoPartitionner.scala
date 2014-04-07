package jobs

import akka.actor.Actor
import models.GeoSquare
import models._

/*TODO maybe add the geolocation too*/
class GeoPartitionner extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*Map holding the results*/
  var results: Map[GeoSquare, Long] = Map()
  
  def receive = {
   case Winner => 
    println("winner is: "+results.maxBy(_._2))
   case Report(id, count) =>
    total += count
    results += (id -> count)
   case TotalTweets => 
    sender ! total
  }
}
