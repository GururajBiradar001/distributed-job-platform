package com.gururaj.workerservice.dto;

import java.util.UUID;

public record JobCreatedEvent (
    UUID jobId,
    String type,
    String payload,
    Integer priority,
    Integer attempts,
    Integer maxAttempts
) {
}
