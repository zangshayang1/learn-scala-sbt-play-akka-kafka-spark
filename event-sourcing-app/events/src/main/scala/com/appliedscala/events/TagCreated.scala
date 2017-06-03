package com.appliedscala.events

import java.util.UUID
import com.appliedscala.events.EventData
import play.api.libs.json.{JsValue, Json, Reads}

case class TagCreated(id: UUID, text: String, createdBy: UUID) extends EventData {
  override def action = TagCreated.actionName
  // it refers to the following TagCreated companion object (singleton)
  override def json = Json.writes[TagCreated].writes(this)
  // "writes" macro from the play.api.libs.json can be used on any case class to build a JSON encoder at COMPILE TIME.
}

object TagCreated {
  val actionName = "tag-created"
  // "tag-created" in the companion object means you can access this static field without initializing such an object

  implicit val reads: Reads[TagCreated] = Json.reads[TagCreated]
  // "reads" macro from play.api.libs.json can be used to restore events from its serialized forms.
  // define an implicit Reads[TagCreated] type of val in scope for deserialize method "as" to use, explained below.
}

/** We design TagCreated this way because we want to do something like this (really defined in InMemoryReadDAO.scala):

  val record: LogRecord = ???
  record.action match {
    case TagCreated.actionName =>             // we can use TagCreate this line and the following line because of the existence of the companion object
      val event = record.data.as[TagCreated]  // def as[T](implicit fjs: Reads[T]) : T --- as method's signature, used to deserialize record.data with a Reads decoder defined in TagCreated singleton.
    case _ =>                                 // whichever Reads decoder is used in as method is decided by the type of object [T] it is called upon, [TagCreated] in this case.
      ...                                     // And the Reads[TagCreated] should be provided in the implicit scope!
}
*/
