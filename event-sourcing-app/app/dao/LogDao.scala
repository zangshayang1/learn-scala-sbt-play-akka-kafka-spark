package dao

import com.appliedscala.events.LogRecord
import scalikejdbc._ // {WrappedResultSet, NameDB} and something else that makes sql work
import scala.util.Try


class LogDao {
  def insertLogRecord(event: LogRecord) : Try[Unit] = Try {
    // Try Usage
    // wrapping everything in a Try block signals that the db operation might fail
    // it will return [Unit] in case of success and [throwable] in case of failure
    import scala.concurrent.ExecutionContext.Implicits.global
    NamedDB('eventstore).localTx { implicit session =>
      val jsonStr = event.data.toString() // (event.data : JsValue) defined in events/LogRecord.scala so .. toString() is necessary.
      // the schema of the following TABLE logs is defined in /conf/evolutions/eventstore/1.sql
      sql"""
        INSERT INTO
          logs(record_id, action_name, event_data, timestamp)
        VALUES(${event.id}, ${event.action}, $jsonStr, ${event.timestamp})
        """.update().apply()
    }
  }
  // get all records from log TABLE ordered by timestamp
  def getLogRecords: Try[Seq[LogRecord]] = Try {
    // in previous modern-web-app project, we only have one table under "conf/evolutions/default/", defined in application.conf as the default DB to use
    // so we used DB.readOnly {} to retrieve data
    // but here we have multiple tables
    // we should lack an implicit context variable, that's supposed to be imported from scalikejdbc._
    import scala.concurrent.ExecutionContext.Implicits.global
    NamedDB('eventstore).readOnly {implicit session =>
      sql"""
        SELECT * FROM logs ORDER BY timestamp
        """.map(rs2LogRecord).list().apply()
    }
  }
  import play.api.libs.json.Json
  import java.util.UUID
  private def rs2LogRecord(rs: WrappedResultSet) = {
    LogRecord(
      UUID.fromString(rs.string("record_id")),
      rs.string("action_name"),
      Json.parse(rs.string("event_data")),
      rs.jodaDateTime("timestamp") // they integrate jodaDateTime even into WrappedResultSet
    )
  }
}
