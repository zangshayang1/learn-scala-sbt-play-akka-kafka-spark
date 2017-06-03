package com.appliedscala.events

import java.util.UUID
import com.appliedscala.events.EventData
import play.api.libs.json.{JsValue, Json, Reads}

/** please refer to TagCreated.scala for explanations.
*/

case class TagDeleted(id: UUID, deletedBy: UUID) extends EventData {
  override def action = TagDeleted.actionName
  override def json = Json.writes[TagDeleted].writes(this)
}

object TagDeleted {
  val actionName = "tag-deleted"
  implicit val reads: Reads[TagDeleted] = Json.reads[TagDeleted]
}
