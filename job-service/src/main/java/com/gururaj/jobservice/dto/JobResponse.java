package com.gururaj.jobservice.dto;

import com.gururaj.jobservice.entity.JobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String type,
        String payload,
        JobStatus status,
        Integer priority,
        Integer attempts,
        Integer maxAttempts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}