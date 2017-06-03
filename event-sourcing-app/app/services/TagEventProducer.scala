package services

import com.appliedscala.events.{EventData, LogRecord}
import scala.concurrent.Future
import akka.actor.ActorSystem
import java.util.UUID
import org.joda.time.DateTime

import model.LogDao
import model.Tag
import scala.util.{Failure, Success}

class TagEventProducer(actorSystem: ActorSystem,
                       logDao: LogDao,
                       readService: ReadService
                       ) {

  private def createLogRecord(eventData: EventData) : LogRecord = {
    // eventData is a trait defined in /events/EventData, extended by TagCreated and TagDeleted
    // eventData.action and eventData.json are two abstract method
    // wrap the serialized instance around with UUID and TimeStamp to form a LogRecord instance
    LogRecord(UUID.randomUUID(),
              eventData.action,
              eventData.json,
              DateTime.now()
              )
  }

  import java.util.concurrent.TimeUnit
  import akka.pattern.ask
  import akka.util.Timeout
  import actors.InMemoryReadActor

  private def adjustReadState(logRecord: LogRecord) : Future[Seq[Tag]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val timeout = Timeout.apply(5, TimeUnit.SECONDS)
    val actor = actorSystem.actorSelection(InMemoryReadActor.path)
    // CAN'T understand the next line. the actor should respond with an "untyped" Map(UUID, Tag)
    (actor ? InMemoryReadActor.ProcessEvent(logRecord)).flatMap { _ => readService.getTags }
  }

  /** This method is intended for controller to call
  */
  def createTag(text: String, createdBy: UUID) : Future[Seq[Tag]] = {
    import com.appliedscala.events.TagCreated
    val tagId = UUID.randomUUID()
    val event = TagCreated(tagId, text, createdBy)
    // event instance - tagCreated created and then equiped with:
    //      3 fields
    //            .tagId,
    //            .text,
    //            .createdBy
    //      2 override methods
    //            .action : String = actionName
    //            .json : JsValue = Json.writes[TagCreated].writes(this)
    val record = createLogRecord(event)
    // have serialization done within this method call and create a LogRecord instance.
    logDao.insertLogRecord(record) match {
      // put the record into DB, no wonder the ExecutionContext implicit is needed.
      case Failure(th) => Future.failed(th)
      // also add it in memory map.
      case Success(_) => adjustReadState(record)
    }
  }

  /** This method is intended for controller to call
  */
  def deleteTag(tagId: UUID, deletedBy: UUID): Future[Seq[Tag]] = {
    import com.appliedscala.events.TagDeleted
    // note the different between instantiate TagCreated and TagDeleted.
    // 1. no randomUUID generated for TagDeleted instance.
    // 2. no "text" field for TagDeleted instance.
    val event = TagDeleted(tagId, deletedBy)
    val record = createLogRecord(event)
    logDao.insertLogRecord(record) match {
      case Failure(th) => Future.failed(th)
      case Success(_) => adjustReadState(record)
    }
  }
}
