output "kafka_topics" {
  description = "Provisioned Kafka topic names"
  value = {
    for key, topic in confluent_kafka_topic.topics : key => topic.topic_name
  }
}

output "ecr_repository_url" {
  description = "ECR repository URL for page-view-aggregator"
  value       = aws_ecr_repository.page_view_aggregator.repository_url
}