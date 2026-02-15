#!/bin/bash
# Wait for Kafka Connect to be ready
echo "Waiting for Kafka Connect to start..."
while ! curl -s http://localhost:8083/connectors > /dev/null 2>&1; do
  sleep 5
done
echo "Kafka Connect is ready."

# Deploy the S3 Sink Connector
echo "Deploying S3 Sink Connector..."
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @/connectors/s3-sink-connector.json

echo "Connector deployed. Checking status..."
curl -s http://localhost:8083/connectors/page-view-s3-sink/status | python3 -m json.tool