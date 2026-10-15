package com.gururaj.jobservice.controller;

import com.gururaj.jobservice.dto.CreateJobRequest;
import com.gururaj.jobservice.dto.JobResponse;
import com.gururaj.jobservice.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/{jobId}/claim")
    public ResponseEntity<Void> claimJob(
            @PathVariable UUID jobId,
            @RequestParam String workerId) {

        boolean claimed =
                jobService.claimJob(jobId, workerId);

        if (!claimed) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(
            @PathVariable UUID jobId) {

        return ResponseEntity.ok(
                jobService.getJob(jobId)
        );
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateJobRequest request) {

        JobResponse response =
                jobService.createJob(request, idempotencyKey);

        return ResponseEntity
                .status(201)
                .body(response);
    }

    @PutMapping("/{jobId}/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @PathVariable UUID jobId,
            @RequestParam String workerId) {

        boolean renewed =
                jobService.renewLease(jobId, workerId);

        if (!renewed) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(
            @PathVariable UUID jobId) {

        boolean cancelled = jobService.cancelJob(jobId);

        if (!cancelled) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/failed")
    public ResponseEntity<Void> markJobFailed(
            @PathVariable UUID jobId,
            @RequestParam String workerId) {

        boolean failed =
                jobService.markJobFailed(jobId, workerId);

        if (!failed) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        return ResponseEntity.ok().build();
    }
}