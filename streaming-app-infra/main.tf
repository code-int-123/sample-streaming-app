terraform {
  required_version = ">= 1.0"

  backend "s3" {
    bucket  = "streaming-app-terraform-state"
    key     = "streaming-app-infra/terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    confluent = {
      source  = "confluentinc/confluent"
      version = "~> 2.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# provider "confluent" {
#   cloud_api_key    = var.confluent_cloud_api_key
#   cloud_api_secret = var.confluent_cloud_api_secret
# }