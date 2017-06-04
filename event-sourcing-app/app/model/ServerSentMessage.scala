package model

import play.api.libs.json.{JsValue, Json, Writes}

case class ServerSentMessage(updateType: String, updateData: JsValue) {
  def json: JsValue = Json.toJson(this)(ServerSentMessage.writes)
}

object ServerSentMessage {
  val writes = Json.writes[ServerSentMessage]

  def create[T](updateType: String, updateData: T)(implicit encoder: Writes[T]) = ServerSentMessage(updateType, encoder.writes(updateData))
}

/*
  The novel thing here is that callers of this method don’t have to provide the
  encoder explicitly as long as the T type has an implicit Writes[T] instance defined.

  so we can do something like:
    val tags: Seq[Tag] = ???
    val ssm = ServerSentMessage.create("tags", tags)

  updateObj -> serialized to JsValue -> wrapped with String updateType -> ServerSentMessageObj
  WTF is this?
  obj -> JSON -> obj... what's the point???

  When client receives the above event, it can check the updateType to know what stucture the updateData field has.

*/
