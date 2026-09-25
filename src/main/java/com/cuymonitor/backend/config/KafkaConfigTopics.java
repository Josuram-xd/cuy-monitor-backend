package com.cuymonitor.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfigTopics {

    public static final String EVENTS_CAMERA = "events.camera";
    public static final String EVENTS_AUDIO = "events.audio";
    public static final String EVENTS_WEIGHT = "events.weight";
    public static final String EVENTS_CAGE = "events.cage";

    @Bean
    NewTopic eventsCameraTopic() {
        return TopicBuilder.name(EVENTS_CAMERA).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic eventsAudioTopic() {
        return TopicBuilder.name(EVENTS_AUDIO).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic eventsWeightTopic() {
        return TopicBuilder.name(EVENTS_WEIGHT).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic eventsCageTopic() {
        return TopicBuilder.name(EVENTS_CAGE).partitions(1).replicas(1).build();
    }
}
