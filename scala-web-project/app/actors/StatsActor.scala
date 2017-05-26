package actors

import akka.actor.Actor
import akka.actor.Actor.Receive


// companion object
object StatsActor {
  val name = "statsActor"
  val path = s"/user/$name"

  // create case object is very common when your message doesn't need any parameters.
  case object Ping
  case object RequestReceived
  case object GetStats
}

import actors.StatsActor.{GetStats, RequestReceived, Ping} // StatsActor is a built in class?

class StatsActor extends Actor {
  var counter = 0

  override def receive: Receive = {
    case Ping => ()
    case RequestReceived => counter += 1
    case GetStats => sender() ! counter
  }
}

/*
For our task, we will need three messages.
During the start-up phase,
the application start-up code will send a Ping message to the actor.

The Ping itself doesn’t do anything other than ensures that
the actor is started and ready to accept other messages.

When a new request comes,
the StatsFilter will send another message called RequestReceived.

The StatsActor reacts to this message by incrementing the counter.

Finally, the Application controller can send a message called GetStats,
and the actor will respond by sending the current value of the counter back.

In addition to these three messages,
the actor needs a name and path.
The name is used during the creation of an actor,
and the path is used for obtaining a reference to it.









*/
