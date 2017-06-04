package services

import akka.actor.ActorSystem
import play.api.Configuration

class ServiceKafkaProducer(topicName: String,
                           actorSystem: ActorSystem, // which will be used by the library
                           configuration: Configuration
                         ) {

  /*
  get bootstrapServers
  */
  val bootstrapServers = configuration.getString("kafka.bootstrap.servers").getOrElse(
    throw new Exception("No config element for 'kafka.bootstrap.servers!'")
  )

  /*
  config and create producer
    StringSerializers are provided. In theory, different serializers can be applied on keys and values separately.
  */
  import akka.kafka.ProducerSettings
  import org.apache.kafka.common.serialization.StringSerializer
  val producerSettings = ProducerSettings(actorSystem, new StringSerializer, new StringSerializer).withBootstrapServers(bootstrapServers)
  val producer = producerSettings.createKafkaProducer()

  import org.apache.kafka.clients.producer.ProducerRecord
  def send(logRecordStr: String) : Unit = {
    producer.send(new ProducerRecord(topicName, logRecordStr))
  }
}
