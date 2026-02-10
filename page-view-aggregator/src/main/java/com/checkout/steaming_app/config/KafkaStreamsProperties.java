package com.checkout.steaming_app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "kafka")
public class KafkaStreamsProperties {

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

    public boolean isSaslSsl() {
        return "SASL_SSL".equalsIgnoreCase(securityProtocol);
    }
}