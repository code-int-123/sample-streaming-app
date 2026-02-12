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

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.page_view_aggregator.id
}

output "ec2_public_ip" {
  description = "EC2 instance public IP"
  value       = aws_instance.page_view_aggregator.public_ip
}