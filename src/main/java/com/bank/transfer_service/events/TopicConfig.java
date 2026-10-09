package com.bank.transfer_service.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic transferCompletedTopic() {
        return TopicBuilder.name("transfer-events")
                .partitions(1) // Set the number of partitions for the topic
                .replicas(1) // Set the number of replicas for the topic
                .configs(Map.of("min.insync.replicas", "2")) // Set the minimum number of in-sync replicas
                .build(); // Build the topic configuration
    }
}
