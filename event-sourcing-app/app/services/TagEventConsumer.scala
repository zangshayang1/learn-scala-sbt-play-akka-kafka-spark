package services

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.Configuration

import com.appliedscala.events.LogRecord
import utils.ServiceKafkaConsumer

class TagEventConsumer(
  readService: ReadService,
  actorSystem: ActorSystem,
  configuration: Configuration,
  materilizer: Materializer) {

  val topicName = "tags"
  val serviceKafkaConsumer = new ServiceKafkaConsumer(Set(topicName), "read", materilizer, actorSystem, configuration, handleEvent)

  private def handleEvent(event: String): Unit = {
    val maybeLogRecord = LogRecord.decode(event)
    maybeLogRecord.foreach(adjustReadState)
  }

  import java.util.concurrent.TimeUnit
  import akka.pattern.ask
  import akka.util.Timeout
  import actors.{InMemoryReadActor, EventStreamActor}
  import model.ServerSentMessage
  private def adjustReadState(logRecord: LogRecord) : Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val timeout = Timeout.apply(5, TimeUnit.SECONDS)
    val actor = actorSystem.actorSelection(InMemoryReadActor.path)
    // CAN'T understand the next line. the actor should respond with an "untyped" Map(UUID, Tag)
    (actor ? InMemoryReadActor.ProcessEvent(logRecord)).foreach { _ =>
      readService.getTags.foreach { tags =>
        val update = ServerSentMessage.create("tags", tags)
        val esActor = actorSystem.actorSelection(EventStreamActor.pathPattern)
        esActor ! EventStreamActor.DataUpdated(update.json)
      }
    }
  }
}
