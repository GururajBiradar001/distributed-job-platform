package com.gururaj.workerservice.publisher;

import com.gururaj.workerservice.dto.JobCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class JobResultPublisher {

    private static final String TOPIC = "job-results";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public JobResultPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishCompleted(UUID jobId, String results) {
        JobCompletedEvent event = new JobCompletedEvent(
                UUID.randomUUID(),
                jobId,
                "JOB_COMPLETED",
                results
        );

        try{
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    TOPIC,
                    jobId.toString(),
                    message
            );

            System.out.println("Published completion event for job: " + jobId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish completion event", e);
        }
    }
}
