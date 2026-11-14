package com.gururaj.jobservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class JobMetrics {

    private final Counter jobsCreated;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter jobsDead;

    public JobMetrics(MeterRegistry registry) {

        jobsCreated = Counter.builder("jobs_created")
                .description("Total number of jobs created")
                .register(registry);

        jobsCompleted = Counter.builder("jobs_completed")
                .description("Total number of jobs completed")
                .register(registry);

        jobsFailed = Counter.builder("jobs_failed")
                .description("Total number of job failures")
                .register(registry);

        jobsDead = Counter.builder("jobs_dead")
                .description("Total number of jobs moved to DEAD")
                .register(registry);
    }

    public void jobCreated() {
        jobsCreated.increment();
    }

    public void jobCompleted() {
        jobsCompleted.increment();
    }

    public void jobFailed() {
        jobsFailed.increment();
    }

    public void jobDead() {
        jobsDead.increment();
    }
}