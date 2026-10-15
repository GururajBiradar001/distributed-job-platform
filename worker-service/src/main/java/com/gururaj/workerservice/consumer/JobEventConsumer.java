package com.gururaj.workerservice.consumer;

import com.gururaj.workerservice.client.JobClaimClient;
import com.gururaj.workerservice.dto.JobCreatedEvent;
import com.gururaj.workerservice.executor.JobExecutor;
import com.gururaj.workerservice.executor.JobExecutorRegistry;
import com.gururaj.workerservice.heartbeat.LeaseHeartbeat;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JobEventConsumer {

    private final ObjectMapper objectMapper;
    private final JobExecutorRegistry executorRegistry;
    private final JobClaimClient jobClaimClient;
    private final LeaseHeartbeat leaseHeartbeat;

    public JobEventConsumer(
            ObjectMapper objectMapper,
            JobExecutorRegistry executorRegistry,
            JobClaimClient jobClaimClient,
            LeaseHeartbeat leaseHeartbeat) {

        this.objectMapper = objectMapper;
        this.executorRegistry = executorRegistry;
        this.jobClaimClient = jobClaimClient;
        this.leaseHeartbeat = leaseHeartbeat;
    }

    @KafkaListener(
            topics = "job-events",
            groupId = "job-workers"
    )
    public void consume(String message) {

        JobCreatedEvent event = null;

        try {

            event =
                    objectMapper.readValue(
                            message,
                            JobCreatedEvent.class
                    );

            boolean claimed =
                    jobClaimClient.claimJob(event.jobId());

            if (!claimed) {
                System.out.println(
                        "Job already claimed: "
                                + event.jobId()
                );
                return;
            }

            System.out.println(
                    "Claimed job: "
                            + event.jobId()
            );

            JobExecutor executor =
                    executorRegistry.getExecutor(event.type());

            if (executor == null) {
                throw new IllegalArgumentException(
                        "No executor found for job type: "
                                + event.type()
                );
            }

            leaseHeartbeat.start(event.jobId());

            try {
                executor.execute(event);
            } finally {
                leaseHeartbeat.stop(event.jobId());
            }

        } catch (Exception e) {

            System.err.println(
                    "Failed to process job event: "
                            + e.getMessage()
            );

            if (event != null) {

                try {

                    boolean failed =
                            jobClaimClient.markJobFailed(
                                    event.jobId()
                            );

                    System.out.println(
                            "Job failure reported: "
                                    + event.jobId()
                                    + " success="
                                    + failed
                    );

                } catch (Exception failureReportingException) {

                    System.err.println(
                            "Failed to report job failure: "
                                    + failureReportingException.getMessage()
                    );
                }
            }

            throw e;
        }
    }
}