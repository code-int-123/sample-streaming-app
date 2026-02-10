package com.checkout.steaming_app.config;

import com.checkout.events.PageViewEvent;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProducerProperties.class)
public class KafkaProducerConfig {

    private final KafkaProducerProperties properties;

    @Bean
    public ProducerFactory<String, PageViewEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, properties.getSchemaRegistryUrl());

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

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, PageViewEvent> kafkaTemplate(ProducerFactory<String, PageViewEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}