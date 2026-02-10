package com.checkout.steaming_app.config;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaStreamsProperties.class)
public class DefaultStreamsConfiguration {

    private final KafkaStreamsProperties properties;

    @Bean
    public Map<String, Object> defaultKafkaStreamsConfig() {
        Map<String, Object> props = new HashMap<>();

        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, properties.getApplicationServer());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, properties.getSchemaRegistryUrl());

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(StreamsConfig.STATE_DIR_CONFIG, properties.getStateDir());

        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                LogAndFailExceptionHandler.class.getName());
        props.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "all");
        props.put(StreamsConfig.topicPrefix(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG),
                properties.getMinInsyncReplicas());
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, properties.getReplicationFactor());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, properties.getCommitIntervalMs());

        if (properties.isSaslSsl()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, properties.getSecurityProtocol());
            props.put(SaslConfigs.SASL_MECHANISM, properties.getSaslMechanism());
            props.put(SaslConfigs.SASL_JAAS_CONFIG, properties.getSaslJaasConfig());
            props.put(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG, properties.getClientDnsLookup());
            props.put(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, properties.getSessionTimeoutMs());
            props.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
                    properties.getBasicAuthCredentialsSource());
            props.put(SchemaRegistryClientConfig.USER_INFO_CONFIG, properties.getBasicAuthUserInfo());
        }

        return props;
    }

    @Bean
    public HostInfo defaultHostStoreInfo() {
        return HostInfo.buildFromEndpoint(properties.getApplicationServer());
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}