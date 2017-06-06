package utils

import akka.actor.ActorSystem
import play.api.Configuration
import akka.stream.Materializer


/*
  In Kafka, each consumer belongs to a particular group.
  --------------------------------------------------
  Why is it important?
  Well, unlike traditional messaging systems, Kafka doesn’t delete events that have been consumed.
  Instead, after a consumer from one particular group consumes a message, it shifts the message offset for its group.
  The message offsets for other groups will remain untouched until they consume the same message and shift the offset as well.

  This allows to have multiple subscribers that see all events and process them independently from each other.
  In our scenario, it makes sense to create two consumers
    - LogRecordConsumer and TagEventConsumer.

  The first one will belong to the log group and subscribe to all known topics.
    That's why topicName is a Set of Strings.
  The second one will subscribe to the tag topic and belong to the read group.

*/
class ServiceKafkaConsumer(topicName: Set[String],
                           groupName: String,
                           implicit val mat: Materializer,
                           actorSystem: ActorSystem,
                           configuration: Configuration,
                           handleEvent: String => Unit) {

  val config = configuration.getConfig("kafka").getOrElse(throw new Exception("No config element for kafka!")).underlying

  import akka.kafka.ConsumerSettings
  import org.apache.kafka.common.serialization.StringDeserializer
  import org.apache.kafka.clients.consumer.ConsumerConfig

  val consumerSettings = ConsumerSettings(actorSystem, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(config.getString("bootstrap.servers"))
    .withGroupId(groupName)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getString("auto.offset.reset")
  )

  import akka.kafka.Subscriptions
  import akka.kafka.scaladsl.Consumer
  import akka.stream.scaladsl.Sink
  import scala.concurrent.Future
  // create an instance of Source[CommittableMessage, Control]
  // final case class CommittableMessage[K, V]( record: ConsumerRecord[K, V], committableOffset: CommittableOffset)
  // .mapAsync(1) extract V and map it to msg => {} function.
  // difference when comparing .mapAsync() and .map() is the former returns Future[T]
  // also notice how we define Producer and Consumer differently?
  // it is because Producer.send() is triggered by user activities while Consumer consumes whatever is in the line.
  // In kafka, Producer can publish events labeled with topic names to their own destinations. Consumer can subscribe to multiple topics.
  // Question: What is the use of GROUP concept???
  Consumer.committableSource(consumerSettings, Subscriptions.topics(topicName)).mapAsync(1) { msg =>
    val event: String = msg.record.value()
    // the record field of msg contains the event encoded as a String, extracted by .value()
    handleEvent(event)
    Future.successful(msg)
  }.mapAsync(1) { msg =>
    msg.committableOffset.commitScaladsl()
      // marks this event as processed in Kafka for this particular consumer group.
  }.runWith(Sink.ignore)

}
