package actors

import akka.actor.Props
import play.api.libs.json.{JsObject, JsValue, JsString}
import java.util.UUID

/*
  This actor
*/
object EventStreamActor {
  def props() = Props(new EventStreamActor)

  // used to trigger a new "server-sent" event to the client
  case class DataUpdated(jsValue: JsValue)
  // used to notify users of possible validation errors
  case class ErrorOccurred(message: String)

  val name = "event-stream-actor"
  val pathPattern = s"/user/$name-*"

  def name(maybeUserId: Option[UUID]) : String = {
    val randomPart = UUID.randomUUID().toString.split("-").apply(0)
    val userPart = maybeUserId.map(_.toString).getOrElse("unregistered")
    s"$name-$userPart-$randomPart"
  }

  def userSpecificPathPattern(userId: UUID) = {
    s"/user/$name-${userId.toString}-*"
  }
}

/*
Since we don’t always deal with logged in users, we are making the user identifier parameter optional.
However, when we do know who the user is, we’re making this identifier part of the actor path.
Later, if we want to deliver a server-sent notification only to a particular user,
  we can use something like this:
    val userId: UUID = ???
    actorSystem.actorSelection(EventStreamActor.userSpecificPathPattern(userId))
*/

import akka.stream.actor.ActorPublisher

class EventStreamActor extends ActorPublisher[JsValue] {
  import EventStreamActor._
  import akka.stream.actor.ActorPublisherMessage._

  override def receive: Receive = {
    // onNext is defined in the super class of ActorPublisher to send the update js to subscribers
    case DataUpdated(js) => onNext(js)
    // make a tuple out of the error message and wrap it in JSON
    case ErrorOccurred(message) => onNext(JsObject(Seq("error" -> JsString(message))))
    // subscribers send Request to signify that they are ready to receive more data.
    // only useful when we had a buffer of updates inside the actor.
    // But here our goal is to always send new updates once they reach the actor.
    case Request(_) => ()
    // If a subscriber send a Cancel, stop the actor.
    case Cancel => context.stop(self)
  }
}
