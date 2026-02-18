package com.checkout.steaming_app.service;

import com.checkout.events.PageViewEvent;
import com.checkout.steaming_app.config.KafkaProducerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class PageViewEventsPublishService {

    private final KafkaTemplate<String, PageViewEvent> kafkaTemplate;
    private final KafkaProducerProperties properties;

    private List<String> postcodes;

    private static final Random RANDOM = new Random();

    public PageViewEventsPublishService(KafkaTemplate<String, PageViewEvent> kafkaTemplate, KafkaProducerProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.postcodes= getPostcodes();
    }

    @PostConstruct
    public void loadPostcodes() throws IOException, InterruptedException {
        log.info("Loaded {} postcodes: {}", postcodes.size(), postcodes);
        int [] count = {0,0,0,0,0};
        for(int i=0;i<10;i++) {
            int index = RANDOM.nextInt(0, 5);
            PageViewEvent event = PageViewEvent.newBuilder()
                    .setPostcode(postcodes.get(index))
                    .setTimestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli())
                    .setUserId(RANDOM.nextInt(0,Integer.MAX_VALUE))
                    .setWebpage("www.sample.com/"+UUID.randomUUID().toString())
                    .build();
            publish(event).join();
            Thread.sleep(1000);
        }
    }


    public CompletableFuture<SendResult<String, PageViewEvent>> publish(PageViewEvent event) {
        String key = event.getPostcode().toString();
        String topic = properties.getTopicName();

        log.info("Publishing PageViewEvent to topic={} key={} postcode={} webpage={}",
                topic, key, event.getPostcode(), event.getWebpage());

        CompletableFuture<SendResult<String, PageViewEvent>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PageViewEvent key={}", key, ex);
            } else {
                log.info("Published PageViewEvent key={} partition={} offset={}",
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    private List<String> getPostcodes(){
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = new ClassPathResource("random-postcode.json").getInputStream()) {
            return mapper.readValue(is, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load postcodes from random-postcode.json", ex);
        }
    }
}