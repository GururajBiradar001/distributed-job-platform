package com.gururaj.jobservice.dto;

import java.util.UUID;

public record JobDeadLetterEvent(
        UUID eventId,
        UUID jobId,
        String eventType,
        String jobType,
        String payload,
        Integer attempts,
        Integer maxAttempts,
        String reason
) {
}