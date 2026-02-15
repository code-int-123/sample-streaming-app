variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "aws_access_key_id" {
  description = "AWS access key ID"
  type        = string
  sensitive   = true
}

variable "aws_secret_access_key" {
  description = "AWS secret access key"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "test"
}

variable "ec2_instance_type" {
  description = "EC2 instance type for page-view-aggregator"
  type        = string
  default     = "t3.micro"
}

variable "ec2_key_pair_name" {
  description = "Name of the EC2 key pair for SSH access"
  type        = string
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