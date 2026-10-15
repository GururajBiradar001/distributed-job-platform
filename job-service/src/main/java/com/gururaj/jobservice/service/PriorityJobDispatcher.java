package com.gururaj.jobservice.service;

import com.gururaj.jobservice.dto.JobCreatedEvent;
import com.gururaj.jobservice.entity.Job;
import com.gururaj.jobservice.entity.JobStatus;
import com.gururaj.jobservice.entity.OutboxEvent;
import com.gururaj.jobservice.repository.JobRepository;
import com.gururaj.jobservice.repository.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class PriorityJobDispatcher {

    private static final long DISPATCH_LOCK_KEY = 1987654322L;

    private final JobRepository jobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public PriorityJobDispatcher(
            JobRepository jobRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {

        this.jobRepository = jobRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void dispatchNextJob() {

        // Serialize dispatchers if multiple Job Service instances run.
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(:lockKey)"
                )
                .setParameter("lockKey", DISPATCH_LOCK_KEY)
                .getSingleResult();

        Optional<Job> candidate =
                jobRepository
                        .findByStatusAndDispatchedFalseOrderByPriorityDescCreatedAtAsc(
                                JobStatus.QUEUED,
                                PageRequest.of(0, 1)
                        )
                        .stream()
                        .findFirst();

        if (candidate.isEmpty()) {
            return;
        }

        Job job = candidate.get();
        LocalDateTime now = LocalDateTime.now();

        JobCreatedEvent event = new JobCreatedEvent(
                job.getId(),
                job.getType(),
                job.getPayload(),
                job.getPriority(),
                job.getAttempts(),
                job.getMaxAttempts()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventType("JOB_DISPATCHED");
            outboxEvent.setAggregateId(job.getId());
            outboxEvent.setPayload(payload);
            outboxEvent.setCreatedAt(now);
            outboxEvent.setPublished(false);

            outboxEventRepository.save(outboxEvent);

            job.setDispatched(true);
            job.setUpdatedAt(now);

            jobRepository.save(job);

            System.out.println(
                    "Dispatched job=" + job.getId()
                            + " priority=" + job.getPriority()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to dispatch job: " + job.getId(),
                    e
            );
        }
    }
}
