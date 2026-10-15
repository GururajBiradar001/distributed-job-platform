package com.gururaj.jobservice.consumer;

import com.gururaj.jobservice.dto.JobCompletedEvent;
import com.gururaj.jobservice.service.JobService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JobResultConsumer {

    private final ObjectMapper objectMapper;
    private final JobService jobService;

    public JobResultConsumer(
            ObjectMapper objectMapper,
            JobService jobService) {
        this.objectMapper = objectMapper;
        this.jobService = jobService;
    }

    @KafkaListener(
            topics = "job-results",
            groupId = "job-result-consumers"
    )
    public void consume(String message) {

        try {
            JobCompletedEvent event =
                    objectMapper.readValue(
                            message,
                            JobCompletedEvent.class
                    );

            System.out.println(
                    "Received completion for job: "
                            + event.jobId()
            );

            jobService.markJobCompleted(event);

        } catch (Exception e) {
            System.err.println(
                    "Failed to process job result: "
                            + e.getMessage()
            );
        }
    }
}