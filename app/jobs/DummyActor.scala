package jobs

import akka.actor.Actor

class DummyActor extends Actor {
 def receive = { 
   case s : String => println(s)
 }
}