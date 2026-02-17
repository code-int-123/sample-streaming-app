#!/bin/bash

# Wait for Kafka Connect to be ready (timeout after 5 minutes)
echo "Waiting for Kafka Connect to start..."
TIMEOUT=300
ELAPSED=0
while ! curl -s http://localhost:8083/connectors > /dev/null 2>&1; do
  sleep 5
  ELAPSED=$((ELAPSED + 5))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "ERROR: Kafka Connect did not start within ${TIMEOUT}s"
    exit 1
  fi
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
curl -s http://localhost:8083/connectors/page-view-s3-sink/status
curl -s http://localhost:8083/connectors/page-view-event-s3-sink/status