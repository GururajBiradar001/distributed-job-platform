package com.gururaj.jobservice.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RetryPolicy {

    private static final long BASE_DELAY_SECONDS = 2;
    private static final long MAX_DELAY_SECONDS = 60;

    public long calculateDelay(int attempt) {
        long exponentialDelay = BASE_DELAY_SECONDS * (1l << (attempt - 1));

        exponentialDelay = Math.min(exponentialDelay,
                MAX_DELAY_SECONDS);

        long jitter = ThreadLocalRandom.current().nextLong(0, 2);

        return exponentialDelay + jitter;
    }
}
