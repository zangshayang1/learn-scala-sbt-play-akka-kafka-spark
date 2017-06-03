package dao

import model.Tag
import java.util.UUID
import com.appliedscala.events.{LogRecord, TagCreated, TagDeleted}

class InMemoryReadDAO(records: Seq[LogRecord]) {
  // create a mutable map responsible for maintaining the current list of tags
  import scala.collection.mutable.{Map => MMap}
  val tags = MMap.empty[UUID, Tag]

  // upon initialization of this InMemoryReadDAO instance, what is the current state of the application?
  // the current state has to be rebuilt in a fast-forward manner
  def init(): Unit = records.foreach(processEvent)

  // very core
  def processEvent(record: LogRecord): Unit = {
    record.action match {

      case TagCreated.actionName =>
        val event = record.data.as[TagCreated]
        // deserializer --- "as" method signature: def as[T](implicit fjs: Reads[T]) : T
        // detailed explanation in events/TagCreated.scala
        tags += (event.id -> Tag(event.id, event.text))

      case TagDeleted.actionName =>
        val event = record.data.as[TagDeleted]
        tags -= event.id

      // have your ever wondered why this method returns Unit? () ? What is Unit type?
      // Unit is the same thing as void in java.
      // tags += (UUID -> Tag) --- so this line of code returns an untyped Map, more specifically: Any = Map(UUID, Tag) in scala console
      // only that makes the use of .flatMap{} reasonable in TagEventProducer.adjustReadState(LogRecord) : Future[Seq[Tag]]
      // INTERESTING
      case _ => ()
    }
  }

  def getTags: Seq[Tag] = {
    tags.values.toList.sortWith(_.text < _.text)
  }
}
