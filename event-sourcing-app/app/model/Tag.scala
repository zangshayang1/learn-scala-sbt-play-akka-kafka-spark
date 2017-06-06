package model

import java.util.UUID
import play.api.libs.json.Json
import org.joda.time.DateTime

// the building block of in-memory representation of "event query" result
case class Tag(id: UUID, text: String, timestamp: DateTime)

/* Questions???

  Previously in TagCreated.scala, the Json serializer is defined directly under the class like this:

    def json = Json.writes[TagCreated].writes(this)

  Also in TagCreated.scala, the Json deserializer is defined with in the Companion object like this:

    implicit val reads: Reads[TagCreated] = Json.reads[TagCreated]

  Why do we do the following? - Guess it is because the way the serializer is invoked.

  (de)serializer invoked on the instance itself:
    In TagEventProducer.scala, TagCreated instance serializer is invoked by --- .json method on the instance itself.

  (de)serializer invoked from built-in methods:
    In InMemoryReadDAO.scala, TagCreated deserializer is invoked via --- record.data.as[TagCreated]
    In TagController.scala, Tag instance serializer is invoked via --- Json.toJson(tag)

  If invoked from built-in methods, they resolve around an implicit Json (de)serializer.
*/


object Tag {
  import play.api.libs.json.Writes
  implicit val writes: Writes[Tag] = Json.writes[Tag]
}
