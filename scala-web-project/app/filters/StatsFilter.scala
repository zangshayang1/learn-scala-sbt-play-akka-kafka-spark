package filters

import play.api.mvc.{Result, RequestHeader, Filter}
import play.api.Logger

import akka.stream.Materializer
import akka.actor.ActorSystem
import actors.StatsActor

import scala.concurrent.Future

class StatsFilter(
  actorSystem : ActorSystem,
  implicit val mat : Materializer) extends Filter {

  // Curried function
  // I guess this is the pattern of defining a filter, you will see why once you understand what it does.
  override def apply(nextFilter : (RequestHeader) => Future[Result])
                    (header : RequestHeader) : Future[Result] = {

              Logger.info(s"Serving another request: ${header.path}")

              // send RequestReceived msg to statsActor()
              actorSystem.actorSelection(StatsActor.path) ! StatsActor.RequestReceived

              // nextFilter is defined as a function {RequestHeader => Future[Result]} provided through 1st argument
              nextFilter(header)
              }
}
