package com.gururaj.jobservice.dto;

import java.util.UUID;

public record JobCompletedEvent(
        UUID eventId,
        UUID jobId,
        String eventType,
        String result
) {
}