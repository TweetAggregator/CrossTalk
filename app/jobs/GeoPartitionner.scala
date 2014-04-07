package jobs

import akka.actor.Actor
import models.GeoSquare
import models._

/*TODO maybe add the geolocation too*/
class GeoPartitionner extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*Map holding the results*/
  var results: Map[(Int, Int), Long] = Map()
  
  def receive = {
   case "Winner" => 
    println("winner is: "+results.maxBy(_._2))
   case Report(id, count) =>
    total += count
    results += (id -> count)
   case "Total" => 
    println("Total received: "+total)
   case "Size" => 
    println ("Map size: "+results.keys.size)
   case _ =>
    println("Boom Boom Boom Boom ")
   }
}
