package com.cuymonitor.backend.ingestion.kafka;

import com.cuymonitor.backend.config.KafkaConfigTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @KafkaListener(topics = {
            KafkaConfigTopics.EVENTS_CAMERA,
            KafkaConfigTopics.EVENTS_AUDIO,
            KafkaConfigTopics.EVENTS_WEIGHT
    })

    public void onEvent(ConsumerRecord<String, String> record) {
        logger.info("Received event topic={} key={} value={}", record.topic(), record.key(), record.value());
    }
}
