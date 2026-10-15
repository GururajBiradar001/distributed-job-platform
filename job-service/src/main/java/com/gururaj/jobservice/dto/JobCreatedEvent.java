package com.gururaj.jobservice.dto;

import java.util.UUID;

public record JobCreatedEvent(
        UUID jobId,
        String type,
        String payload,
        Integer priority,
        Integer attempts,
        Integer maxAttempts
) {
}