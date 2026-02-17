FROM confluentinc/cp-kafka-connect:7.7.1

# Install the S3 Sink Connector and Avro converter
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-s3:10.5.17
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-avro-converter:7.7.1

ENV CONNECT_PLUGIN_PATH="/usr/share/java,/usr/share/confluent-hub-components"

# Copy connector configs and startup script
USER root
COPY connectors/ /connectors/
COPY --chmod=755 start.sh /start.sh
USER appuser

CMD bash -c "/start.sh & /etc/confluent/docker/run"