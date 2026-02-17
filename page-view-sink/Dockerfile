FROM confluentinc/cp-kafka-connect:7.7.1

# Install the S3 Sink Connector and Avro converter
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-s3:10.5.17
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-avro-converter:7.7.1

ENV CONNECT_PLUGIN_PATH="/usr/share/java,/usr/share/confluent-hub-components"

# Copy custom partitioner JAR into S3 connector lib directory
COPY custom-s3-partitioner.jar /usr/share/confluent-hub-components/confluentinc-kafka-connect-s3/lib/

# Copy connector configs and startup script
USER root
COPY connectors/ /connectors/
COPY --chmod=755 start.sh /start.sh
USER appuser

# start.sh polls for readiness and registers connectors in the background
# /etc/confluent/docker/run is the main Kafka Connect process (PID 1)
CMD bash -c "/start.sh & /etc/confluent/docker/run"