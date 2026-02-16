package com.checkout.steaming_app.config;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.AutoOffsetReset;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.streams.KafkaStreamsMicrometerListener;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Data
@Validated
@Configuration
@EnableKafka
@ConfigurationProperties(prefix = "kafka")
@EnableConfigurationProperties
public class KafkaStreamsConfig {

    @NotBlank
    private String applicationId;

    @NotBlank
    private String stateDir;

    @NotBlank
    private String bootstrapServers;

    @NotBlank
    private String schemaRegistryUrl;

    @NotBlank
    private String applicationServer;

    private String securityProtocol;

    private String saslMechanism;

    private String saslJaasConfig;

    private String clientDnsLookup;

    private Integer sessionTimeoutMs;

    private String basicAuthCredentialsSource;

    private String basicAuthUserInfo;

    @NotNull
    @Positive
    private Integer replicationFactor;

    @NotNull
    @Positive
    private Integer minInsyncReplicas;

    @NotNull
    @PositiveOrZero
    private Long commitIntervalMs;

    @NotBlank
    private String pageViewInputTopic;

    @NotBlank
    private String pageViewOutputTopic;

    public boolean isSaslSsl() {
        return "SASL_SSL".equalsIgnoreCase(securityProtocol);
    }

    @Bean
    public Map<String, Object> defaultKafkaStreamsConfig() {
        Map<String, Object> props = new HashMap<>();

        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, applicationServer);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);

        props.put(StreamsConfig.DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                LogAndFailExceptionHandler.class.getName());
        props.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "all");
        props.put(StreamsConfig.topicPrefix(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG), minInsyncReplicas);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, replicationFactor);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, commitIntervalMs);

        if (isSaslSsl()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
            props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
            props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
            props.put(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG, clientDnsLookup);
            props.put(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
            props.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, basicAuthCredentialsSource);
            props.put(SchemaRegistryClientConfig.USER_INFO_CONFIG, basicAuthUserInfo);
        }

        return props;
    }

    @Bean
    public StreamsBuilderFactoryBean streamsBuilderFactoryBean(
            KafkaStreamsConfiguration kafkaStreamsConfiguration,
            MeterRegistry meterRegistry) {
        StreamsBuilderFactoryBean factoryBean = new StreamsBuilderFactoryBean(kafkaStreamsConfiguration);
        factoryBean.addListener(new KafkaStreamsMicrometerListener(meterRegistry));
        return factoryBean;
    }

    @Bean
    public KafkaStreamsConfiguration kafkaStreamsConfiguration() {
        Map<String, Object> config = defaultKafkaStreamsConfig();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        config.put(StreamsConfig.CLIENT_ID_CONFIG, applicationId + "-client");
        config.put(
                StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
                AutoOffsetReset.earliest().toString().toLowerCase(Locale.ROOT));
        return new KafkaStreamsConfiguration(config);
    }

    @Bean
    public HostInfo defaultHostStoreInfo() {
        return HostInfo.buildFromEndpoint(applicationServer);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}