# steaming-app

A streaming data pipeline that publishes page view events to Kafka, aggregates them with Kafka Streams, and archives results to AWS S3.

## Architecture

```
page-view-events-publisher
        │
        ▼
topic: test.streaming.page-view (Confluent Cloud)
        │
        ├──────────────────────────────┐
        ▼                              ▼
page-view-aggregator          page-view-sink (raw)
(Kafka Streams)               (Kafka Connect → S3)
        │
        ▼
topic: test.streaming.page-view.output
        │
        ▼
page-view-sink (aggregated)
(Kafka Connect → S3)
```

### Modules

| Module | Description |
|--------|-------------|
| `page-view-events` | Avro schema definitions (`PageViewEvent`, `AggregatedPageViewEvent`) |
| `page-view-events-publisher` | Spring Boot producer — publishes sample page view events to Kafka |
| `page-view-aggregator` | Spring Boot Kafka Streams app — groups by postcode, counts views in 1-minute tumbling windows |
| `page-view-sink` | Confluent Kafka Connect container — S3 sink for both raw events and aggregated output |
| `streaming-app-infra` | Terraform — Confluent topics, ECR repos, EC2 instances, S3 buckets, IAM, CloudWatch |

## Stream Processing

The aggregator reads `PageViewEvent` records, groups by postcode, and applies a **1-minute tumbling window with a 30-minute grace period**. Each window emits an `AggregatedPageViewEvent` containing the postcode, page view count, window start time, and aggregate interval. Results are written to the output topic keyed by postcode.

State is backed by RocksDB and retained for 1 hour.

## Prerequisites

- Java 21
- Maven
- Docker
- AWS CLI (for deployment)
- Terraform 1.9+ (for infrastructure)
- A Confluent Cloud cluster with Schema Registry

## Local Development

Build all modules:

```bash
mvn clean package
```

Build skipping tests:

```bash
mvn clean package -DskipTests
```

Run the publisher (publishes 10 sample events on startup):

```bash
cd page-view-events-publisher
mvn spring-boot:run
```

Run the aggregator:

```bash
cd page-view-aggregator
mvn spring-boot:run
```

## Configuration

Both Spring Boot applications read Kafka credentials from environment variables.

| Variable | Description |
|----------|-------------|
| `KAFKA_SASL_JAAS_CONFIG` | Confluent Kafka SASL JAAS config string |
| `SCHEMA_REGISTRY_USER_INFO` | Schema Registry credentials in `key:secret` format |
| `AWS_REGION` | AWS region for CloudWatch metrics export (aggregator only) |

Example:

```bash
export KAFKA_SASL_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="KEY" password="SECRET";'
export SCHEMA_REGISTRY_USER_INFO="KEY:SECRET"
export AWS_REGION="us-east-1"
```

Topic names and other Kafka settings are in each module's `src/main/resources/application.yaml`.

## Infrastructure

Terraform provisions:

- **Confluent Cloud** — `test.streaming.page-view` and `test.streaming.page-view.output` topics (2 partitions, 1-day retention)
- **ECR** — `page-view-aggregator` and `page-view-sink` repositories (image scanning enabled)
- **EC2** — `t3.micro` for the aggregator, `t3.small` for the Kafka Connect sink
- **S3** — `test-page-view-sink-output` (aggregated results) and `test-page-view-raw-output` (raw events)
- **IAM** — EC2 roles with ECR pull, S3 write, and CloudWatch permissions
- **CloudWatch** — Dashboard `test-jvm-metrics` with JVM heap, GC, CPU, and thread metrics

State is stored in S3 (`streaming-app-terraform-state`) with encryption.

```bash
cd streaming-app-infra
terraform init
terraform plan
terraform apply
```

Required Terraform variables:

| Variable | Description |
|----------|-------------|
| `aws_access_key_id` | AWS access key |
| `aws_secret_access_key` | AWS secret key |
| `ec2_key_pair_name` | EC2 SSH key pair name |
| `confluent_environment_id` | Confluent environment ID |
| `confluent_cluster_id` | Confluent cluster ID |
| `confluent_cluster_rest_endpoint` | Confluent cluster REST endpoint |
| `confluent_kafka_api_key` | Confluent API key |
| `confluent_kafka_api_secret` | Confluent API secret |

## CI/CD

GitHub Actions (`.github/workflows/streaming-app-infra.yml`) runs on push to `main` or pull requests touching relevant paths:

1. **Terraform Plan** — validates and previews infrastructure changes
2. **Terraform Apply** — provisions infrastructure
3. **Build page-view-aggregator** — Maven build → Docker image → push to ECR
4. **Deploy to EC2** — pulls latest image, restarts container with `awslogs` log driver
5. **Build page-view-sink** — builds Confluent Connect image with S3 plugin → push to ECR
6. **Deploy sink to EC2** — pulls latest image, restarts container, auto-registers S3 connectors

Required GitHub secrets:

```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
EC2_SSH_PRIVATE_KEY
EC2_KEY_PAIR_NAME
CONFLUENT_KAFKA_API_KEY
CONFLUENT_KAFKA_API_SECRET
SCHEMA_REGISTRY_USER_INFO
```

## S3 Output Layout

**Aggregated results** (`test-page-view-sink-output`) — partitioned by event time:
```
year=2025/month=01/day=15/hour=10/minute=30/
```

**Raw events** (`test-page-view-raw-output`) — partitioned by postcode:
```
postcode=SW1A1/
postcode=EC2R8/
```

## Monitoring

The aggregator exports JVM and Kafka Streams metrics to CloudWatch under the `PageViewAggregator` namespace every 60 seconds. The `test-jvm-metrics` dashboard displays heap memory, GC pause, CPU usage, and thread counts.

Application logs are shipped to CloudWatch Logs:
- `/ec2/page-view-aggregator`
- `/ec2/page-view-sink`

## Tech Stack

| Technology | Version | Use |
|------------|---------|-----|
| Java | 21 | Runtime |
| Spring Boot | 4.0.2 | Application framework |
| Apache Kafka Streams | (BOM) | Stream processing |
| Apache Avro | 1.12.0 | Event schemas |
| Confluent Platform | 8.1.1 / 7.7.1 | Avro serializers, Kafka Connect |
| RocksDB | 9.7.3 | Kafka Streams state store |
| Micrometer CloudWatch2 | (BOM) | Metrics export |
| Terraform | 1.9 | Infrastructure as code |
