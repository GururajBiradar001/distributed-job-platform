package com.gururaj.jobservice.service;

import com.gururaj.jobservice.dto.CreateJobRequest;
import com.gururaj.jobservice.dto.JobCompletedEvent;
import com.gururaj.jobservice.dto.JobDeadLetterEvent;
import com.gururaj.jobservice.dto.JobResponse;
import com.gururaj.jobservice.entity.Job;
import com.gururaj.jobservice.entity.JobStatus;
import com.gururaj.jobservice.entity.OutboxEvent;
import com.gururaj.jobservice.exception.JobNotFoundException;
import com.gururaj.jobservice.observability.JobMetrics;
import com.gururaj.jobservice.publisher.DeadLetterPublisher;
import com.gururaj.jobservice.repository.InboxEventRepository;
import com.gururaj.jobservice.repository.JobRepository;
import com.gururaj.jobservice.repository.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private final InboxEventRepository inboxEventRepository;
    private final JobRepository jobRepository;
    private final RetryPolicy retryPolicy;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final DeadLetterPublisher deadLetterPublisher;
    private final JobMetrics jobMetrics;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${jobs.max-queued:100}")
    private long maxQueuedJobs;

    public JobService(
            JobRepository jobRepository,
            RetryPolicy retryPolicy,
            InboxEventRepository inboxEventRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            DeadLetterPublisher deadLetterPublisher,
            JobMetrics jobMetrics) {

        this.jobRepository = jobRepository;
        this.retryPolicy = retryPolicy;
        this.inboxEventRepository = inboxEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.deadLetterPublisher = deadLetterPublisher;
        this.jobMetrics = jobMetrics;
    }

    // --------------------------------------------------
    // 1. CREATE JOB
    // --------------------------------------------------

    @Transactional
    public JobResponse createJob(
            CreateJobRequest request,
            String idempotencyKey) {

        // Check whether this request was already processed.
        Optional<Job> existingJob =
                jobRepository.findByIdempotencyKey(idempotencyKey);

        if (existingJob.isPresent()) {

            Job job = existingJob.get();

            return new JobResponse(
                    job.getId(),
                    job.getType(),
                    job.getPayload(),
                    job.getStatus(),
                    job.getPriority(),
                    job.getAttempts(),
                    job.getMaxAttempts(),
                    job.getCreatedAt(),
                    job.getUpdatedAt()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // Reject scheduling requests that specify a past time.
        if (request.scheduledAt() != null
                && request.scheduledAt().isBefore(now)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Scheduled time must not be in the past."
            );
        }

        boolean scheduled =
                request.scheduledAt() != null
                        && request.scheduledAt().isAfter(now);

        // Apply queue admission control only to immediately queued jobs.
        if (!scheduled) {

            entityManager.createNativeQuery(
                    "SELECT pg_advisory_xact_lock(1987654321)"
            ).getResultList();

            long queuedJobs =
                    jobRepository.countByStatus(JobStatus.QUEUED);

            if (queuedJobs >= maxQueuedJobs) {

                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Job queue is full. Please try again later."
                );
            }
        }

        Job job = new Job();

        job.setType(request.type());
        job.setPayload(request.payload());

        job.setStatus(
                scheduled
                        ? JobStatus.SCHEDULED
                        : JobStatus.QUEUED
        );

        job.setIdempotencyKey(idempotencyKey);

        job.setPriority(request.priority());
        job.setAttempts(0);
        job.setMaxAttempts(request.maxAttempts());

        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setScheduledAt(request.scheduledAt());

        // Priority dispatcher handles event creation.
        job.setDispatched(false);

        Job savedJob = jobRepository.save(job);

        jobMetrics.jobCreated();

        return new JobResponse(
                savedJob.getId(),
                savedJob.getType(),
                savedJob.getPayload(),
                savedJob.getStatus(),
                savedJob.getPriority(),
                savedJob.getAttempts(),
                savedJob.getMaxAttempts(),
                savedJob.getCreatedAt(),
                savedJob.getUpdatedAt()
        );
    }

    // --------------------------------------------------
    // 2. ACTIVATE SCHEDULED JOBS
    // --------------------------------------------------

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void activateDueScheduledJobs() {

        LocalDateTime now = LocalDateTime.now();

        List<Job> dueJobs =
                jobRepository.findDueScheduledJobs(
                        JobStatus.SCHEDULED,
                        now
                );

        for (Job job : dueJobs) {

            // Make the job eligible for priority-based dispatch.
            job.setStatus(JobStatus.QUEUED);
            job.setDispatched(false);
            job.setUpdatedAt(now);

            jobRepository.save(job);

            System.out.println(
                    "Scheduled job activated: " + job.getId()
            );
        }
    }

    // --------------------------------------------------
    // 3. MARK JOB COMPLETED
    // --------------------------------------------------

    @Transactional
    public void markJobCompleted(JobCompletedEvent event) {

        LocalDateTime now = LocalDateTime.now();

        // Insert the event only if it has not been processed before.
        boolean inserted = inboxEventRepository.insertIfNotExists(
                event.eventId(),
                event.eventType(),
                event.jobId(),
                now
        );

        if (!inserted) {

            System.out.println(
                    "Duplicate event ignored: " + event.eventId()
            );

            return;
        }

        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() ->
                        new JobNotFoundException(
                                "Job not found: " + event.jobId()
                        )
                );

        job.setStatus(JobStatus.COMPLETED);
        job.setWorkerId(null);
        job.setLeaseExpiresAt(null);
        job.setNextRetryAt(null);
        job.setUpdatedAt(now);

        jobRepository.save(job);

        jobMetrics.jobCompleted();

        System.out.println(
                "Job completed successfully: " + job.getId()
        );
    }

    // --------------------------------------------------
    // 4. CLAIM JOB
    // --------------------------------------------------

    @Transactional
    public boolean claimJob(UUID jobId, String workerId) {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime leaseExpiresAt =
                now.plusSeconds(30);

        int updatedRows =
                jobRepository.claimJob(
                        jobId,
                        JobStatus.QUEUED,
                        JobStatus.RUNNING,
                        workerId,
                        leaseExpiresAt,
                        now
                );

        return updatedRows == 1;
    }

    // --------------------------------------------------
    // 5. RENEW WORKER LEASE
    // --------------------------------------------------

    @Transactional
    public boolean renewLease(
            UUID jobId,
            String workerId) {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime newLeaseExpiresAt =
                now.plusSeconds(30);

        int updatedRows =
                jobRepository.renewLease(
                        jobId,
                        JobStatus.RUNNING,
                        workerId,
                        newLeaseExpiresAt,
                        now
                );

        return updatedRows == 1;
    }

    // --------------------------------------------------
    // 6. RECOVER EXPIRED JOBS
    // --------------------------------------------------

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void recoverExpiredJobs() {

        LocalDateTime now = LocalDateTime.now();

        List<Job> expiredJobs =
                jobRepository.findExpiredLeases(
                        JobStatus.RUNNING,
                        now
                );

        System.out.println(
                "Recovery check: found "
                        + expiredJobs.size()
                        + " expired jobs"
        );

        for (Job job : expiredJobs) {

            // Count the failed execution attempt.
            job.setAttempts(job.getAttempts() + 1);

            // Release ownership from the failed worker.
            job.setWorkerId(null);
            job.setLeaseExpiresAt(null);
            job.setUpdatedAt(now);

            if (job.getAttempts() >= job.getMaxAttempts()) {

                // No attempts remain.
                job.setStatus(JobStatus.DEAD);
                job.setNextRetryAt(null);

                System.out.println(
                        "Job exceeded max attempts: "
                                + job.getId()
                );

            } else {

                // Make the recovered job eligible for dispatch again.
                job.setStatus(JobStatus.QUEUED);
                job.setDispatched(false);
                job.setNextRetryAt(null);

                System.out.println(
                        "Recovered expired job: "
                                + job.getId()
                                + " attempt="
                                + job.getAttempts()
                );
            }

            jobRepository.save(job);
        }
    }

    // --------------------------------------------------
    // 7. CANCEL JOB
    // --------------------------------------------------

    @Transactional
    public boolean cancelJob(UUID jobId) {

        LocalDateTime now = LocalDateTime.now();

        int updatedRows = jobRepository.cancelJob(
                jobId,
                JobStatus.CANCELLED,
                List.of(
                        JobStatus.SCHEDULED,
                        JobStatus.QUEUED
                ),
                now
        );

        if (updatedRows == 1) {

            System.out.println(
                    "Job cancelled: " + jobId
            );

            return true;
        }

        if (!jobRepository.existsById(jobId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Job not found: " + jobId
            );
        }

        // The job exists but is not in a cancellable state.
        return false;
    }

    // --------------------------------------------------
    // 8. MARK JOB FAILED
    // --------------------------------------------------

    @Transactional
    public boolean markJobFailed(UUID jobId, String workerId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new JobNotFoundException(
                                "Job not found: " + jobId
                        )
                );

        if (job.getStatus() != JobStatus.RUNNING) {
            return false;
        }

        if (!workerId.equals(job.getWorkerId())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Count this failed execution attempt.
        int nextAttempt = job.getAttempts() + 1;

        job.setAttempts(nextAttempt);
        job.setUpdatedAt(now);

        // Release the failed worker's ownership.
        job.setWorkerId(null);
        job.setLeaseExpiresAt(null);
        jobMetrics.jobFailed();

        // No more attempts are available.
        if (nextAttempt >= job.getMaxAttempts()) {

            job.setStatus(JobStatus.DEAD);
            job.setNextRetryAt(null);

            jobRepository.save(job);
            jobMetrics.jobDead();

            deadLetterPublisher.publish(
                    job,
                    "Maximum retry attempts exceeded"
            );

            System.out.println(
                    "Job moved to DEAD: "
                            + job.getId()
                            + " attempts="
                            + nextAttempt
            );

            return true;
        }

        // Calculate when the job becomes eligible for retry.
        long delaySeconds =
                retryPolicy.calculateDelay(nextAttempt);

        LocalDateTime nextRetryAt =
                now.plusSeconds(delaySeconds);

        job.setStatus(JobStatus.RETRYING);
        job.setNextRetryAt(nextRetryAt);

        jobRepository.save(job);
        jobMetrics.jobFailed();

        System.out.println(
                "Job scheduled for retry: "
                        + job.getId()
                        + " attempt="
                        + nextAttempt
                        + " retryAt="
                        + nextRetryAt
                        + " delay="
                        + delaySeconds
                        + "s"
        );

        return true;
    }

    // --------------------------------------------------
    // 9. PROCESS READY RETRIES
    // --------------------------------------------------

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processReadyRetries() {

        LocalDateTime now = LocalDateTime.now();

        List<Job> retryJobs =
                jobRepository.findJobsReadyForRetry(
                        JobStatus.RETRYING,
                        now
                );

        for (Job job : retryJobs) {

            // Requeue the job for priority-based dispatch.
            job.setStatus(JobStatus.QUEUED);
            job.setDispatched(false);
            job.setNextRetryAt(null);
            job.setUpdatedAt(now);

            jobRepository.save(job);

            System.out.println(
                    "Retrying job: "
                            + job.getId()
                            + " attempt="
                            + job.getAttempts()
            );
        }
    }

    // --------------------------------------------------
    // 10. GET JOB
    // --------------------------------------------------

    public JobResponse getJob(UUID jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new JobNotFoundException(
                                "Job not found: " + jobId
                        )
                );

        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getPayload(),
                job.getStatus(),
                job.getPriority(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}