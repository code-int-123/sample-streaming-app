package com.checkout.steaming_app.topology;

import com.checkout.events.AggregatedPageViewEvent;
import com.checkout.events.PageViewEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.apache.kafka.streams.state.WindowStore;
import com.checkout.steaming_app.config.KafkaStreamsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class PageViewTopology {

    private static final String STATE_STORE_NAME = "page-view-count-by-postcode-store";
    private static final int WINDOW_SIZE_SECONDS = 60;

    private final KafkaStreamsConfig kafkaProperties;

    @Bean
    public KStream<String, PageViewEvent> pageViewAggregationTopology(
            StreamsBuilder streamsBuilder) {

        KStream<String, PageViewEvent> stream = streamsBuilder
                .stream(
                kafkaProperties.getPageViewInputTopic(),
                Consumed.<String, PageViewEvent>as("page-view-input")
                        .withTimestampExtractor(new PageViewEventTimestampExtractor()));

        TimeWindows tumblingWindow = TimeWindows.ofSizeAndGrace(Duration.ofMinutes(1), Duration.ofMinutes(30));

        stream
                .groupByKey()
                .windowedBy(tumblingWindow)
                .count(Materialized.<String, Long, WindowStore<org.apache.kafka.common.utils.Bytes, byte[]>>as(STATE_STORE_NAME)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long())
                        .withRetention(Duration.ofHours(1)))
                .toStream()
                .mapValues((windowedKey, count) -> AggregatedPageViewEvent.newBuilder()
                        .setPostcode(windowedKey.key())
                        .setPageViewCount(count.intValue())
                        .setAggregateIntervalInSeconds(WINDOW_SIZE_SECONDS)
                        .setAggregationWindow(Instant.ofEpochMilli(windowedKey.window().start()))
                        .build())
                .selectKey((windowedKey, value) -> windowedKey.key())
                .to(kafkaProperties.getPageViewOutputTopic());

        return stream;
    }

    public static class PageViewEventTimestampExtractor implements TimestampExtractor {
        @Override
        public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
            if (record.value() instanceof PageViewEvent event) {
                return event.getTimestamp();
            }
            return partitionTime;
        }
    }
}
