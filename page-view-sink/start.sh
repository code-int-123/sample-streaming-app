#!/bin/bash

# Start Kafka Connect in the background
/etc/confluent/docker/run &

# Wait for Kafka Connect to be ready
echo "Waiting for Kafka Connect to start..."
while ! curl -s http://localhost:8083/connectors > /dev/null 2>&1; do
  sleep 5
done
echo "Kafka Connect is ready."

# Deploy the aggregated page view S3 Sink Connector
echo "Deploying aggregated page view S3 Sink Connector..."
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @/connectors/s3-sink-connector.json

# Deploy the raw page view S3 Sink Connector
echo "Deploying raw page view S3 Sink Connector..."
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @/connectors/page-view-raw-s3-sink-connector.json

echo "Connectors deployed. Checking status..."
curl -s http://localhost:8083/connectors/page-view-s3-sink/status | python3 -m json.tool
curl -s http://localhost:8083/connectors/page-view-event-s3-sink/status | python3 -m json.tool

# Keep container alive by waiting for Kafka Connect
wait