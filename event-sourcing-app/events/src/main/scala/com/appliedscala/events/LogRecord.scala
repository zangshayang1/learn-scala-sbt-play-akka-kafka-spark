package com.appliedscala.events

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsValue, Json}

/**
A general record class that wraps around "events".
Its fields correspond to the column layout of "logs" table.
*/
case class LogRecord(id: UUID,
                     action: String,
                     data: JsValue,
                     timestamp: DateTime) {
  def encode: String = { Json.toJson(this)(LogRecord.writes).toString() }
  // Json.toJson() -> what do you want? Serialize it into Json.
  // Json.toJson(this) -> what do you want to serialize? this object.
  // Json.toJson(this)(serializer) -> how do you want to serialize it?
  // Json.toJson(this)(serializer).toString -> JsValue to String
}

object LogRecord {
  val writes = Json.writes[LogRecord] // make a serializer
  val reads = Json.reads[LogRecord] // make a deserializer
  def decode(str: String): Option[LogRecord] = {
    Json.parse(str).asOpt[LogRecord](reads)
  }
}

/*
Encoding LogRecord values as strings is a two-step process.
  First, we need to convert a value into JSON using the LogRecord.writes serializer.
  Then, we can call toString to turn the JsValue into text.

The opposite thing happens during decoding.
  We take a string and try to deserialize it into a LogRecord.
  If it fails, we return None;
  if it succeeds, we return a log record wrapped in Some
*/
