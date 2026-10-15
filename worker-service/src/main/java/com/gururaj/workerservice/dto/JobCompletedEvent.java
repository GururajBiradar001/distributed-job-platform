package com.gururaj.workerservice.dto;

import java.util.UUID;

public record JobCompletedEvent(
        UUID eventId,
        UUID jobId,
        String eventType,
        String result
) {}