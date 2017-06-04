package services

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.Configuration

import dao.LogDao
import com.appliedscala.events.LogRecord
import utils.ServiceKafkaConsumer

class LogRecordConsumer(logDao: LogDao,
                        actorSystem: ActorSystem,
                        configuration: Configuration,
                        materializer: Materializer) {

  val topics = Seq("tags").toSet
  val serviceKafkaConsumer = new ServiceKafkaConsumer(topics, "log", materializer, actorSystem, configuration, handleEvent)

  private def handleEvent(event: String): Unit = {
    val maybeGenericEnvelope = LogRecord.decode(event)
    maybeGenericEnvelope.foreach { envelope =>
      logDao.insertLogRecord(envelope)
    }
  }

}
