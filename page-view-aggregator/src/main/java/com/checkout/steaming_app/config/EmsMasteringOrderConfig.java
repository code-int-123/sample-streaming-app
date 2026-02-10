package com.checkout.steaming_app.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.streams.KafkaStreamsMicrometerListener;

import java.util.Locale;
import java.util.Map;

@Configuration
@EnableKafka
public class EmsMasteringOrderConfig {

    public static final String STREAMS_CONFIG_BEAN_NAME = "emsOrderMasteringKafkaStreamConfig";
    public static final String STREAMS_BUILDER_BEAN_NAME = "emsOrderMasteringKafkaStreamBuilder";


    private final KafkaStreamsProperties properties;

    @Autowired
    public EmsMasteringOrderConfig( KafkaStreamsProperties properties) {
        this.properties = properties;
    }

    @Bean(name = STREAMS_BUILDER_BEAN_NAME)
    public StreamsBuilderFactoryBean emsMasteringStreamBuilder(
            KafkaStreamsConfiguration orderMasterStreamsConfig,
            MeterRegistry meterRegistry,
            ApplicationEventPublisher publisher) {
        StreamsBuilderFactoryBean streamsBuilderFactoryBean =
                new StreamsBuilderFactoryBean(orderMasterStreamsConfig);
        streamsBuilderFactoryBean.addListener(new KafkaStreamsMicrometerListener(meterRegistry));
//        streamsBuilderFactoryBean.setStateListener(
//                new StreamStateListener(emsOrderMasteringKafkaStreamConfig, publisher));
        return streamsBuilderFactoryBean;
    }

    @Bean(name = STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration emsMasteringKafkaStreamConfig(
            DefaultStreamsConfiguration defaultStreamsConfig) {

        Map<String, Object> config = defaultStreamsConfig.defaultKafkaStreamsConfig();

        config.put(StreamsConfig.APPLICATION_ID_CONFIG, properties.getApplicationId());
        config.put(StreamsConfig.CLIENT_ID_CONFIG, properties.getApplicationId() + "-client");

        // Where do you want to start from when the application is deployed for the first time OR its offsets are reset?
        // Earliest (beginning of the topic) OR latest (most recent records)? Normally EARLIEST is the right answer
        config.put(
                StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
                Topology.AutoOffsetReset.EARLIEST.toString().toLowerCase(Locale.ROOT));

        return new KafkaStreamsConfiguration(config);
    }
}
