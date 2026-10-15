package com.gururaj.workerservice.heartbeat;

import com.gururaj.workerservice.client.JobClaimClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LeaseHeartbeat {

    private final JobClaimClient jobClaimClient;

    private final Set<UUID> activeJobs =
            ConcurrentHashMap.newKeySet();

    public LeaseHeartbeat(JobClaimClient jobClaimClient) {
        this.jobClaimClient = jobClaimClient;
    }

    public void start(UUID jobId) {
        activeJobs.add(jobId);
    }

    public void stop(UUID jobId) {
        activeJobs.remove(jobId);
    }

    @Scheduled(fixedRate = 10_000)
    public void renewLeases() {

        for (UUID jobId : activeJobs) {

            boolean renewed =
                    jobClaimClient.renewLease(jobId);

            if (!renewed) {
                System.out.println(
                        "Lease renewal failed for job: "
                                + jobId
                );

                activeJobs.remove(jobId);
                continue;
            }

            System.out.println(
                    "Lease renewed for job: "
                            + jobId
            );
        }
    }
}