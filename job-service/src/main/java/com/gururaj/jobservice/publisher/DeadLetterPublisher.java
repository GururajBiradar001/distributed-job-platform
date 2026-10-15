package com.gururaj.jobservice.publisher;

import com.gururaj.jobservice.dto.JobDeadLetterEvent;
import com.gururaj.jobservice.entity.Job;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class DeadLetterPublisher {

    private static final String TOPIC = "job-dead-letter";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DeadLetterPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Job job, String reason) {

        JobDeadLetterEvent event =
                new JobDeadLetterEvent(
                        UUID.randomUUID(),
                        job.getId(),
                        "JOB_DEAD",
                        job.getType(),
                        job.getPayload(),
                        job.getAttempts(),
                        job.getMaxAttempts(),
                        reason
                );

        try {

            String message =
                    objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    TOPIC,
                    job.getId().toString(),
                    message
            );

            System.out.println(
                    "Published dead-letter event for job: "
                            + job.getId()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to publish dead-letter event",
                    e
            );
        }
    }
}