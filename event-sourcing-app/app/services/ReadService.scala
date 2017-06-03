package services

import model.Tag
import dao.LogDao
import scala.util.Try
import akka.actor.ActorSystem
import play.api.Logger

import actors.InMemoryReadActor
import scala.util.{Failure, Success}

class ReadService(actorSystem: ActorSystem, logDao: LogDao) {

  init()
  // when ReadService is initialized, all the records in TABLE log will be retrieved via LogDao and they will be referenced by logRecords upon successful return
  private def init(): Unit = {
    val logRecordsTry = logDao.getLogRecords // retrieve from TABLE log via a logDAO

    logRecordsTry match {
      case Failure(th) =>
        Logger.error("Error while initializing the read service", th)
        throw th // th: throwable???
      case Success(logRecords) =>
        // initialize an actor from actorSystem according to the "profiling singleton of this actor" --- InMemoryReadActor
        // pass in the retrieved logRecords, so that an InMemoryReadDAO instance can be built within this actor
        val actor = actorSystem.actorOf(InMemoryReadActor.props(logRecords), InMemoryReadActor.name)
        // send the actor with "InitializeState" msg, defined again in the "profiling singleton of this actor" --- InMemoryReadActor
        // the actor reacts to the msg by invoking InMemoryReadDAO instance's init() method, which loads everything in logRecords into an in-memory Map
        // now, because the actor has the InMemoryReadDAO instance as a field, it can response to queries promptly.
        actor ! InMemoryReadActor.InitializeState
    }
  }

  import java.util.concurrent.TimeUnit
  import akka.util.Timeout
  import scala.concurrent.Future
  import akka.pattern.ask // ask pattern needs to be imported explicitly.
  def getTags: Future[Seq[Tag]] = {
    // a Timeout val must be provided within the implicit scope for ask pattern to work with
    implicit val timeout = Timeout.apply(5, TimeUnit.SECONDS)
    // note: actorSystem.actorOf() vs. actorSystem.actorSelection()
    val actor = actorSystem.actorSelection(InMemoryReadActor.path)
    // .mapTo([T]) automatically returns Future[T] since it could fail.
    (actor ? InMemoryReadActor.GetTags).mapTo[Seq[Tag]]
  }

}
