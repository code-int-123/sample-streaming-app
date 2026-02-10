package com.checkout.steaming_app.topology;

import com.checkout.events.AggregatedPageViewEvent;
import com.checkout.events.PageViewEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
@Slf4j
public class PageViewTopology {

    private static final String DISCARD_DUPLICATES =
            "check-and-discard-duplicates---%s".formatted(PageViewTopology.class.getSimpleName());
    private static final String TRANSFORM_DISCARD_DUPLICATES = "---transform---%s".formatted(DISCARD_DUPLICATES);
    private static final String MAP_COMMAND_TO_MASTERED =
            "map-command-to-mastered---%s".formatted(PageViewTopology.class.getSimpleName());
    private static final String TRANSFORM_MAP_COMMAND_TO_MASTERED =
            "---transform---%s".formatted(MAP_COMMAND_TO_MASTERED);
    private static final String FORCE_SYNC_MASTERED_TO_STATE_STORE =
            "force-sync-mastered-to-state-store---%s".formatted(PageViewTopology.class.getSimpleName());
    private static final String TRANSFORM_FORCE_SYNC_MASTERED_TO_STATE_STORE =
            "---transform---%s".formatted(FORCE_SYNC_MASTERED_TO_STATE_STORE);
    private static final String FILTER_NON_NULL_1 =
            "---filter---non-null---%s-%s".formatted(PageViewTopology.class.getSimpleName(), 1);
    private static final String FILTER_NON_NULL_2 =
            "---filter---non-null---%s-%s".formatted(PageViewTopology.class.getSimpleName(), 2);
    private static final String MASTERED_OUTPUT_KEY_RE_KEY_DESCRIPTION =
            "mastered-to-output-key---%s".formatted(PageViewTopology.class.getSimpleName());
    private static final String SELECT_KEY_MASTERED_OUTPUT_KEY_RE_KEY_DESCRIPTION =
            "---re-key---%s".formatted(MASTERED_OUTPUT_KEY_RE_KEY_DESCRIPTION);
    private static final String PEEK_TEMPLATE = "---peek---produce-%s---from-%s---to-%s";

    // INPUT
//    private final String changeEmsOrderCommandTopicName;
//
//    // ERROR
//    private final String changeEmsOrderCommandFailedTopicName;
//
//    // OUTPUT
//    private final String emsOrderMasteredTopicName;
//
//    private final String emsOrderMasteredStateStoreName;
//    private final String emsOrderMasteredHashAccumulatorStateStoreName;
//    private final String orphanCancelEmsOrderCommandStateStoreName;

//    public EmsOrderMasteringTopology(
//            OrderMasterKafkaTopics kafkaTopics, StateStoreBuilderFactory stateStoreBuilderFactory) {
//        this.changeEmsOrderCommandTopicName = kafkaTopics.getMaster().getChangeEmsOrderByIncomingEmsOrderIdTopic();
//        this.changeEmsOrderCommandFailedTopicName =
//                kafkaTopics.getMaster().getChangeEmsOrderFailedByIncomingEmsOrderIdTopic();
//        this.emsOrderMasteredTopicName = kafkaTopics.getMaster().getEmsOrderMasteredTopic();
//        this.emsOrderMasteredStateStoreName = kafkaTopics.getStateStores().getEmsOrderMasteredStoreName();
//        this.orphanCancelEmsOrderCommandStateStoreName =
//                kafkaTopics.getStateStores().getOrphanCancelEmsOrderCommandStateStoreName();
//        this.emsOrderMasteredHashAccumulatorStateStoreName =
//                kafkaTopics.getStateStores().getEmsOrderMasteredHashAccumulatorStateStoreName();
//        this.stateStoreBuilderFactory = stateStoreBuilderFactory;
//    }


    @Bean
    public KStream<String, PageViewEvent> emsOrderMasteringTopologyStreamConfigurer(
            StreamsBuilder emsOrderMasteringKafkaStreamBuilder) {

//        emsOrderMasteringKafkaStreamBuilder.addStateStore(stateStoreBuilderFactory.emsOrderStateStore());
//        emsOrderMasteringKafkaStreamBuilder.addStateStore(
//                stateStoreBuilderFactory.orphanCancelEmsOrderCommandStateStore());
//        emsOrderMasteringKafkaStreamBuilder.addStateStore(
//                stateStoreBuilderFactory.emsOrderMasteredHashAccumulatorStateStore());

        KStream<String, PageViewEvent> stream =
                emsOrderMasteringKafkaStreamBuilder.stream("test.streaming.page-view");

        stream.mapValues(v->AggregatedPageViewEvent
                .newBuilder().setPageViewCount(1)
                .setPostcode(v.getPostcode())
                .setAggregateIntervalInSeconds(60)
                .setAggregationWindow(LocalDateTime.now().toInstant(ZoneOffset.UTC)).build())
                .to("test.streaming.page-view.output");

        return stream;

//        emsOrderStateStoreLoader.loadStream(stream);
//
//        KStream<String, ChangeEmsOrder> changeEmsOrderCommandStream =
//                emsOrderMasteringKafkaStreamBuilder.stream(changeEmsOrderCommandTopicName);
//
//        changeEmsOrderCommandStream
//                .transformValues(
//                        kafkaStreamsTracing.valueTransformerWithKey(
//                                TRANSFORM_DISCARD_DUPLICATES,
//                                () -> new EmsOrderMasteredDuplicateDetectingTransformer(
//                                        emsOrderMasteredHashAccumulatorStateStoreName)),
//                        Named.as(TRANSFORM_DISCARD_DUPLICATES),
//                        emsOrderMasteredHashAccumulatorStateStoreName)
//                .filter((k, v) -> v != null, Named.as(FILTER_NON_NULL_1))
//                .flatTransformValues(
//                        kafkaStreamsTracing.valueTransformerWithKey(
//                                TRANSFORM_MAP_COMMAND_TO_MASTERED,
//                                () -> new ChangeEmsOrderCommandTransformer(
//                                        emsOrderMasteredStateStoreName,
//                                        orphanCancelEmsOrderCommandStateStoreName,
//                                        emsOrderMasteredMapper,
//                                        emsOrderMasteredOrphanCancelMapper)),
//                        Named.as(TRANSFORM_MAP_COMMAND_TO_MASTERED),
//                        orphanCancelEmsOrderCommandStateStoreName,
//                        emsOrderMasteredStateStoreName)
//                .filter((k, v) -> v != null, Named.as(FILTER_NON_NULL_2))
//                .transformValues(
//                        kafkaStreamsTracing.valueTransformerWithKey(
//                                TRANSFORM_FORCE_SYNC_MASTERED_TO_STATE_STORE,
//                                () -> new EmsOrderMasteredForceSyncToStateStoreTransformer(
//                                        emsOrderMasteredStateStoreName)),
//                        Named.as(TRANSFORM_FORCE_SYNC_MASTERED_TO_STATE_STORE),
//                        emsOrderMasteredStateStoreName)
//                .selectKey(toHubIdentifier(), Named.as(SELECT_KEY_MASTERED_OUTPUT_KEY_RE_KEY_DESCRIPTION))
//                .transformValues(
//                        kafkaStreamsTracing.peek(
//                                PEEK_TEMPLATE.formatted(
//                                        EmsOrderMastered.class.getSimpleName(),
//                                        EmsOrderMasteringTopology.class.getSimpleName(),
//                                        emsOrderMasteredTopicName),
//                                (k, v) -> log.info(
//                                        "{} produced to {}: k={} tpeid={}",
//                                        EmsOrderMastered.class.getSimpleName(),
//                                        emsOrderMasteredTopicName,
//                                        k,
//                                        v.getIngestionEmsOrderIdentifier())),
//                        Named.as(PEEK_TEMPLATE.formatted(
//                                EmsOrderMastered.class.getSimpleName(),
//                                EmsOrderMasteringTopology.class.getSimpleName(),
//                                emsOrderMasteredTopicName)))
//                .to(emsOrderMasteredTopicName);

//        return changeEmsOrderCommandStream;

    }

//    private static KeyValueMapper<String, EmsOrderMastered, String> toHubIdentifier() {
//        return (k, v) -> v.getIdentifier();
//    }
}
