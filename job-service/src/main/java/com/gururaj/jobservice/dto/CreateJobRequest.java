package com.gururaj.jobservice.dto;

import java.time.LocalDateTime;

public record CreateJobRequest(
        String type,
        String payload,
        Integer priority,
        Integer maxAttempts,
        LocalDateTime scheduledAt
) {
}