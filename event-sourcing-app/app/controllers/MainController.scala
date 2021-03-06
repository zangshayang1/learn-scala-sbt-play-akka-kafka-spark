package controllers

import java.util.UUID

import model._
import play.api.libs.json.JsValue
import play.api.mvc._
import security.{UserAuthAction, UserAwareAction, UserAwareRequest}
import services.ConsumerAggregator

import akka.actor.ActorSystem
import akka.stream.Materializer


class MainController(userAuthAction: UserAuthAction,
                     userAwareAction: UserAwareAction,
                     actorSystem: ActorSystem,
                     consumerAggregator: ConsumerAggregator,
                     mat: Materializer
                   ) extends Controller {

  import actors.EventStreamActor
  import akka.stream.actor.ActorPublisher // trait ActorPublisher[T] extends Actor { def totalDemand: Long = ???; def onNext(element: T): Unit = ???}
  import play.api.libs.EventSource
  import akka.stream.scaladsl._ // scala data stream library?

  // After we start publishing events to a Kafka topic, we will no longer be able to use simple Futures for notifying clients about state updates.
  // We just need to introduce a new endpoint that sends events to a client in the text/event-stream format and initialize this long-living connection when the client loads.
  // In Akka Stream, we will publish events to a Source on the server side.
  // They will go through a Flow represented as EventSource.flow, and finally they will be received by a client that initiated the event-stream connection.
  def serverEventStream = userAwareAction { request =>
    implicit val materializer = mat
    implicit val actorFactor = actorSystem

    val maybeUser = request.user
    val maybeUserId = maybeUser.map(_.userId) // ? access request property this way???
    val actorRef = actorSystem.actorOf(EventStreamActor.props(), EventStreamActor.name(maybeUserId))

    // object Source {def fromPublisher[T](publisher: Publisher[T]): Source[T, NotUsed]}
    // create a new actor publisher
    val eventStorePublisher = Source.fromPublisher(ActorPublisher[JsValue](actorRef)).runWith(Sink.asPublisher(fanout = true))
    val source = Source.fromPublisher(eventStorePublisher)
    // connect this actor to the Play facilities for sending server-sent events
    Ok.chunked(source.via(EventSource.flow)).as("text/event-stream")
  }


  def index = userAwareAction { request =>
    Ok(views.html.pages.react(buildNavData(request),
      WebPageData("Home")))
  }

  def error500 = Action {
    InternalServerError(views.html.errorPage())
  }

  private def buildNavData(request: UserAwareRequest[_]): NavigationData = {
    NavigationData(request.user, isLoggedIn = request.user.isDefined)
  }
}
