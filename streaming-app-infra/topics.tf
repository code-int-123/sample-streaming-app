locals {
  kafka_topics = {
    page_view = {
      name       = "${var.environment}.streaming.page-view"
      partitions = 2
    }
    page_view_output = {
      name       = "${var.environment}.streaming.page-view.output"
      partitions = 2
    }
  }
}

resource "confluent_kafka_topic" "topics" {
  for_each = local.kafka_topics

  kafka_cluster {
    id = var.confluent_cluster_id
  }

  topic_name       = each.value.name
  partitions_count = each.value.partitions

  config = {
    "cleanup.policy" = "delete"
    "retention.ms"   = "86400000" # 1 days
  }

  rest_endpoint = var.confluent_cluster_rest_endpoint

  credentials {
    key    = var.confluent_kafka_api_key
    secret = var.confluent_kafka_api_secret
  }
}