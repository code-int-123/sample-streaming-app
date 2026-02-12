variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "test"
}

# Confluent Cloud

# variable "confluent_cloud_api_key" {
#   description = "Confluent Cloud API key"
#   type        = string
#   sensitive   = true
# }
#
# variable "confluent_cloud_api_secret" {
#   description = "Confluent Cloud API secret"
#   type        = string
#   sensitive   = true
# }

variable "confluent_environment_id" {
  description = "Confluent Cloud environment ID (e.g. env-xxxxx)"
  type        = string
}

variable "confluent_cluster_id" {
  description = "Confluent Cloud Kafka cluster ID (e.g. lkc-xxxxx)"
  type        = string
}

variable "confluent_cluster_rest_endpoint" {
  description = "Confluent Cloud Kafka cluster REST endpoint"
  type        = string
}

variable "confluent_kafka_api_key" {
  description = "Kafka API key for topic management"
  type        = string
  sensitive   = true
}

variable "confluent_kafka_api_secret" {
  description = "Kafka API secret for topic management"
  type        = string
  sensitive   = true
}