output "kafka_topics" {
  description = "Provisioned Kafka topic names"
  value = {
    for key, topic in confluent_kafka_topic.topics : key => topic.topic_name
  }
}