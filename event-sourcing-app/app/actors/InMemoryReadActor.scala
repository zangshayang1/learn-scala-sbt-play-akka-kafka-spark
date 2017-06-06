package actors

import akka.actor.{Actor, Props}
import com.appliedscala.events.LogRecord

/**
  define an "actor profiling singleton" for InMemoryReadActor
*/
object InMemoryReadActor {
  val name = "in-memory-read-actor" // The name is used during the creation of an actor, specifically actor.Props(new InMemoryReadActor(), InMemoryReadActor.name)
  val path = s"/user/$name" // the path is used for obtaining a reference to it.

  // define messages here noting that -
  // the singleton "msg" doesn't take argument and thus defined as object, meaning one instance is enough for the app
  // while the ProcessEvent msg is defined as class, taking argument, allowing different such msg instances should bounce around
  case class ProcessEvent(event: LogRecord)
  case object InitializeState
  case object GetTags

  // this is like a constructor of the actual instance of InMemoryReadActor defined in the following class
  // in previous modern-web-app project, the instantiation of actor is "wired" directly through Props() in appLoader.scala
  // but here we built the constructor because we want to specify arguments???
  // because you want this actor to hold all the logRecords in memory
  // however by using kafka, we let kafka servers to hold all the logRecords instead
  def props(logRecords: Seq[LogRecord]) = Props(new InMemoryReadActor(logRecords))
}

class InMemoryReadActor(logRecords: Seq[LogRecord]) extends Actor {
  import InMemoryReadActor._ // import messages from the above singleton

  import dao.InMemoryReadDAO
  val readDao = new InMemoryReadDAO(logRecords)

  // override Actor's receive method
  override def receive: Receive = {
    case InitializeState => readDao.init()
    case GetTags => sender() ! readDao.getTags
    case ProcessEvent(event) => sender() ! readDao.processEvent(event)
  }
}
