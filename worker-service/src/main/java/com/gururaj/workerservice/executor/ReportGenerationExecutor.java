package com.gururaj.workerservice.executor;

import com.gururaj.workerservice.dto.JobCreatedEvent;
import com.gururaj.workerservice.publisher.JobResultPublisher;
import org.springframework.stereotype.Component;

@Component
public class ReportGenerationExecutor implements JobExecutor {

    private final JobResultPublisher jobResultPublisher;

    public ReportGenerationExecutor(
            JobResultPublisher jobResultPublisher) {
        this.jobResultPublisher = jobResultPublisher;
    }

    @Override
    public String getJobType() {
        return "REPORT_GENERATION";
    }

    @Override
    public void execute(JobCreatedEvent event) {

        System.out.println(
                "Executing job: " + event.jobId()
        );

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Job interrupted", e);
        }

        String result =
                "Report generated successfully for job "
                        + event.jobId();

        jobResultPublisher.publishCompleted(
                event.jobId(),
                result
        );

        System.out.println(
                "Job execution completed: "
                        + event.jobId()
        );
    }
//    @Override
//    public void execute(JobCreatedEvent event) {
//
//        System.out.println(
//                "Executing job: " + event.jobId()
//        );
//
//        // Simulate actual report generation.
//        String result =
//                "Report generated successfully for job "
//                        + event.jobId();
//
//        jobResultPublisher.publishCompleted(
//                event.jobId(),
//                result
//        );
//
//        System.out.println(
//                "Job execution completed: "
//                        + event.jobId()
//        );
//    }
}