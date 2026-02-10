package com.checkout.steaming_app.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "kafka")
public class KafkaProducerProperties {

    @NotBlank
    private String bootstrapServers;

    @NotBlank
    private String schemaRegistryUrl;

    @NotBlank
    private String topicName;

    private String securityProtocol;

    private String saslMechanism;

    private String saslJaasConfig;

    private String clientDnsLookup;

    private Integer sessionTimeoutMs;

    private String basicAuthCredentialsSource;

    private String basicAuthUserInfo;

    public boolean isSaslSsl() {
        return "SASL_SSL".equalsIgnoreCase(securityProtocol);
    }
}