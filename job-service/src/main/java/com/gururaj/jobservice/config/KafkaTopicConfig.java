package com.gururaj.jobservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic jobEventsTopic() {
        return TopicBuilder.name("job-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jobResultsTopic() {
        return TopicBuilder.name("job-results")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jobDeadLetterTopic() {
        return TopicBuilder.name("job-dead-letter")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
