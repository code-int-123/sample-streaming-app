package com.checkout.steaming_app.topology;

import com.checkout.events.AggregatedPageViewEvent;
import com.checkout.events.PageViewEvent;
import com.checkout.steaming_app.config.KafkaStreamsConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PageViewTopologyIntegrationTest {

    private static final String INPUT_TOPIC = "test.streaming.page-view";
    private static final String OUTPUT_TOPIC = "test.streaming.page-view.output";
    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://test-schema-registry";

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, PageViewEvent> inputTopic;
    private TestOutputTopic<String, AggregatedPageViewEvent> outputTopic;

    @BeforeEach
    void setUp() {
        Map<String, String> schemaRegistryConfig = Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL,
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, "true");

        SpecificAvroSerde<PageViewEvent> inputValueSerde = new SpecificAvroSerde<>();
        inputValueSerde.configure(schemaRegistryConfig, false);

        SpecificAvroSerde<AggregatedPageViewEvent> outputValueSerde = new SpecificAvroSerde<>();
        outputValueSerde.configure(schemaRegistryConfig, false);

        // Build topology
        KafkaStreamsConfig kafkaConfig = new KafkaStreamsConfig();
        kafkaConfig.setPageViewInputTopic(INPUT_TOPIC);
        kafkaConfig.setPageViewOutputTopic(OUTPUT_TOPIC);

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        PageViewTopology topology = new PageViewTopology(kafkaConfig);
        topology.pageViewAggregationTopology(streamsBuilder);

        // Configure streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        testDriver = new TopologyTestDriver(streamsBuilder.build(), props);

        inputTopic = testDriver.createInputTopic(
                INPUT_TOPIC, Serdes.String().serializer(), inputValueSerde.serializer());

        outputTopic = testDriver.createOutputTopic(
                OUTPUT_TOPIC, Serdes.String().deserializer(), outputValueSerde.deserializer());
    }

    @AfterEach
    void tearDown() {
        if (testDriver != null) {
            testDriver.close();
        }
    }

    @Test
    void shouldAggregatePageViewsByPostcodeWithinWindow() {
        Instant windowStart = Instant.parse("2025-01-01T10:00:00Z");

        // Send 3 events for postcode "SW1" within the same 1-minute window
        pipePageViewEvent("SW1", 1, "/home", windowStart);
        pipePageViewEvent("SW1", 2, "/about", windowStart.plusSeconds(10));
        pipePageViewEvent("SW1", 3, "/contact", windowStart.plusSeconds(30));

        // Send 2 events for postcode "EC1" within the same window
        pipePageViewEvent("EC1", 4, "/home", windowStart.plusSeconds(5));
        pipePageViewEvent("EC1", 5, "/products", windowStart.plusSeconds(20));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        // Verify SW1 aggregation
        List<AggregatedPageViewEvent> sw1Results = results.stream()
                .filter(kv -> "SW1".equals(kv.key))
                .map(kv -> kv.value)
                .toList();
        assertThat(sw1Results).isNotEmpty();
        assertThat(sw1Results.getLast().getPageViewCount()).isEqualTo(3);
        assertThat(sw1Results.getLast().getAggregateIntervalInSeconds()).isEqualTo(60);
        assertThat(sw1Results.getLast().getPostcode().toString()).isEqualTo("SW1");
        assertThat(sw1Results.getLast().getAggregationWindow()).isEqualTo(windowStart);

        // Verify EC1 aggregation
        List<AggregatedPageViewEvent> ec1Results = results.stream()
                .filter(kv -> "EC1".equals(kv.key))
                .map(kv -> kv.value)
                .toList();
        assertThat(ec1Results).isNotEmpty();
        assertThat(ec1Results.getLast().getPageViewCount()).isEqualTo(2);
    }

    @Test
    void shouldAggregateInSeparateWindowsForDifferentTimeIntervals() {
        Instant firstWindowStart = Instant.parse("2025-01-01T10:00:00Z");
        Instant secondWindowStart = Instant.parse("2025-01-01T10:01:00Z");

        // 2 events in first window
        pipePageViewEvent("SW1", 1, "/home", firstWindowStart);
        pipePageViewEvent("SW1", 2, "/about", firstWindowStart.plusSeconds(20));

        // 1 event in second window
        pipePageViewEvent("SW1", 3, "/contact", secondWindowStart.plusSeconds(10));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        List<AggregatedPageViewEvent> sw1Results = results.stream()
                .filter(kv -> "SW1".equals(kv.key))
                .map(kv -> kv.value)
                .toList();

        // Should have results from two different windows
        long distinctWindows = sw1Results.stream()
                .map(e -> e.getAggregationWindow().toEpochMilli())
                .distinct()
                .count();
        assertThat(distinctWindows).isEqualTo(2);

        // First window count should be 2
        List<AggregatedPageViewEvent> firstWindowResults = sw1Results.stream()
                .filter(e -> e.getAggregationWindow().equals(firstWindowStart))
                .toList();
        assertThat(firstWindowResults.getLast().getPageViewCount()).isEqualTo(2);

        // Second window count should be 1
        List<AggregatedPageViewEvent> secondWindowResults = sw1Results.stream()
                .filter(e -> e.getAggregationWindow().equals(secondWindowStart))
                .toList();
        assertThat(secondWindowResults.getLast().getPageViewCount()).isEqualTo(1);
    }

    @Test
    void shouldCountLateArrivingEventsWithinGracePeriod() {
        Instant windowStart = Instant.parse("2025-01-01T10:00:00Z");
        Instant nextWindow = Instant.parse("2025-01-01T10:01:00Z");

        // Send events in first window
        pipePageViewEvent("SW1", 1, "/home", windowStart);
        pipePageViewEvent("SW1", 2, "/about", windowStart.plusSeconds(30));

        // Advance time to next window
        pipePageViewEvent("SW1", 3, "/contact", nextWindow.plusSeconds(5));

        // Late event for the first window (within 30min grace period)
        pipePageViewEvent("SW1", 4, "/late", windowStart.plusSeconds(45));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        // The first window should eventually have count 3 (2 on-time + 1 late)
        List<AggregatedPageViewEvent> firstWindowResults = results.stream()
                .filter(kv -> "SW1".equals(kv.key))
                .map(kv -> kv.value)
                .filter(e -> e.getAggregationWindow().equals(windowStart))
                .toList();
        assertThat(firstWindowResults.getLast().getPageViewCount()).isEqualTo(3);
    }

    @Test
    void shouldDropEventsOutsideGracePeriod() {
        Instant windowStart = Instant.parse("2025-01-01T10:00:00Z");
        Instant farFuture = Instant.parse("2025-01-01T11:00:00Z");

        // Send event in first window
        pipePageViewEvent("SW1", 1, "/home", windowStart);

        // Advance stream time well past the grace period (30 min)
        pipePageViewEvent("EC1", 2, "/other", farFuture);

        // Late event for the first window — outside 30min grace, should be dropped
        pipePageViewEvent("SW1", 3, "/late", windowStart.plusSeconds(10));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        List<AggregatedPageViewEvent> sw1FirstWindow = results.stream()
                .filter(kv -> "SW1".equals(kv.key))
                .map(kv -> kv.value)
                .filter(e -> e.getAggregationWindow().equals(windowStart))
                .toList();

        // Count should remain 1 — the late event was dropped
        assertThat(sw1FirstWindow.getLast().getPageViewCount()).isEqualTo(1);
    }

    @Test
    void shouldOutputCorrectKeyAsPostcode() {
        Instant windowStart = Instant.parse("2025-01-01T10:00:00Z");

        pipePageViewEvent("N1", 1, "/home", windowStart);
        pipePageViewEvent("E2", 2, "/about", windowStart.plusSeconds(5));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        // Output key should match the postcode
        results.forEach(kv ->
                assertThat(kv.key).isEqualTo(kv.value.getPostcode().toString()));
    }

    @Test
    void shouldHandleMultiplePostcodesAcrossMultipleWindows() {
        Instant window1 = Instant.parse("2025-01-01T10:00:00Z");
        Instant window2 = Instant.parse("2025-01-01T10:01:00Z");
        Instant window3 = Instant.parse("2025-01-01T10:02:00Z");

        // Window 1: SW1=2, EC1=1
        pipePageViewEvent("SW1", 1, "/home", window1);
        pipePageViewEvent("EC1", 2, "/home", window1.plusSeconds(10));
        pipePageViewEvent("SW1", 3, "/about", window1.plusSeconds(20));

        // Window 2: SW1=1, EC1=2
        pipePageViewEvent("EC1", 4, "/products", window2);
        pipePageViewEvent("SW1", 5, "/contact", window2.plusSeconds(15));
        pipePageViewEvent("EC1", 6, "/checkout", window2.plusSeconds(30));

        // Window 3: EC1=1
        pipePageViewEvent("EC1", 7, "/home", window3.plusSeconds(5));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        // SW1 window 1 final count = 2
        assertThat(lastCountFor(results, "SW1", window1)).isEqualTo(2);
        // EC1 window 1 final count = 1
        assertThat(lastCountFor(results, "EC1", window1)).isEqualTo(1);
        // SW1 window 2 final count = 1
        assertThat(lastCountFor(results, "SW1", window2)).isEqualTo(1);
        // EC1 window 2 final count = 2
        assertThat(lastCountFor(results, "EC1", window2)).isEqualTo(2);
        // EC1 window 3 final count = 1
        assertThat(lastCountFor(results, "EC1", window3)).isEqualTo(1);
    }

    @Test
    void shouldEmitIncrementalCountUpdates() {
        Instant windowStart = Instant.parse("2025-01-01T10:00:00Z");

        pipePageViewEvent("SW1", 1, "/home", windowStart);
        pipePageViewEvent("SW1", 2, "/about", windowStart.plusSeconds(10));
        pipePageViewEvent("SW1", 3, "/contact", windowStart.plusSeconds(20));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        List<Integer> counts = results.stream()
                .filter(kv -> "SW1".equals(kv.key))
                .map(kv -> kv.value.getPageViewCount())
                .toList();

        // Each event should emit an updated count: 1, 2, 3
        assertThat(counts).containsExactly(1, 2, 3);
    }

    @Test
    void shouldSetAggregationWindowToWindowStartTime() {
        Instant windowStart = Instant.parse("2025-01-01T10:00:00Z");

        // Event at 10:00:25 — should be in the 10:00:00 window
        pipePageViewEvent("SW1", 1, "/home", windowStart.plusSeconds(25));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        assertThat(results).hasSize(1);
        // aggregationWindow should be the window start (10:00:00), not the event time (10:00:25)
        assertThat(results.getFirst().value.getAggregationWindow()).isEqualTo(windowStart);
    }

    @Test
    void shouldCountRecentLateEventsAndDropEventsOutsideGracePeriod() {
        Instant now = Instant.parse("2025-01-01T10:00:30Z");
        Instant tenSecondsAgo = now.minusSeconds(10);       // 10:00:20 — same window [10:00:00, 10:01:00)
        Instant oneHourAgo = now.minus(Duration.ofHours(1)); // 09:00:30 — outside 30min grace period

        // Initial event at "now"
        pipePageViewEvent("SW1", 1, "/home", now);

        // 2 late events from 10s ago (within same window and grace period — should be counted)
        pipePageViewEvent("SW1", 2, "/about", tenSecondsAgo);
        pipePageViewEvent("SW1", 3, "/contact", tenSecondsAgo.plusSeconds(2));

        // 2 late events from 1 hour ago (outside 30min grace period — should be dropped)
        pipePageViewEvent("SW1", 4, "/old-page", oneHourAgo);
        pipePageViewEvent("SW1", 5, "/old-page-2", oneHourAgo.plusSeconds(5));

        List<KeyValue<String, AggregatedPageViewEvent>> results = outputTopic.readKeyValuesToList();

        Instant currentWindow = Instant.parse("2025-01-01T10:00:00Z");
        Instant oldWindow = Instant.parse("2025-01-01T09:00:00Z");

        // Current window should have 3 events (1 initial + 2 from 10s ago)
        assertThat(lastCountFor(results, "SW1", currentWindow)).isEqualTo(3);

        // Old window should have 0 events (both dropped — outside grace period)
        assertThat(lastCountFor(results, "SW1", oldWindow)).isEqualTo(0);
    }

    @Test
    void shouldProduceNoOutputWhenNoInput() {
        assertThat(outputTopic.isEmpty()).isTrue();
    }

    private int lastCountFor(List<KeyValue<String, AggregatedPageViewEvent>> results,
                             String postcode, Instant windowStart) {
        return results.stream()
                .filter(kv -> postcode.equals(kv.key))
                .map(kv -> kv.value)
                .filter(e -> e.getAggregationWindow().equals(windowStart))
                .reduce((first, second) -> second)
                .map(AggregatedPageViewEvent::getPageViewCount)
                .orElse(0);
    }

    private void pipePageViewEvent(String postcode, int userId, String webpage, Instant timestamp) {
        PageViewEvent event = PageViewEvent.newBuilder()
                .setUserId(userId)
                .setPostcode(postcode)
                .setWebpage(webpage)
                .setTimestamp(timestamp.toEpochMilli())
                .build();
        inputTopic.pipeInput(postcode, event, timestamp);
    }
}