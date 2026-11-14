package com.gururaj.jobservice.repository;

import com.gururaj.jobservice.entity.Job;
import com.gururaj.jobservice.entity.JobStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    long countByStatus(JobStatus status);

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = :newStatus,
                j.workerId = :workerId,
                j.leaseExpiresAt = :leaseExpiresAt,
                j.updatedAt = :now
            WHERE j.id = :jobId
              AND j.status = :expectedStatus
            """)
    int claimJob(
            @Param("jobId") UUID jobId,
            @Param("expectedStatus") JobStatus expectedStatus,
            @Param("newStatus") JobStatus newStatus,
            @Param("workerId") String workerId,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
            @Param("now") LocalDateTime now
    );

    Page<Job> findByStatusAndDispatchedFalseOrderByPriorityDescCreatedAtAsc(
            JobStatus status,
            Pageable pageable
    );

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("""
        UPDATE Job j
        SET j.leaseExpiresAt = :newLeaseExpiresAt,
            j.updatedAt = :now
        WHERE j.id = :jobId
          AND j.status = :runningStatus
          AND j.workerId = :workerId
          AND j.leaseExpiresAt > :now
        """)
    int renewLease(
            @Param("jobId") UUID jobId,
            @Param("runningStatus") JobStatus runningStatus,
            @Param("workerId") String workerId,
            @Param("newLeaseExpiresAt") LocalDateTime newLeaseExpiresAt,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT j
        FROM Job j
        WHERE j.status = :status
          AND j.leaseExpiresAt < :now
        """)
    List<Job> findExpiredLeases(
            @Param("status") JobStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT j
        FROM Job j
        WHERE j.status = :status
          AND j.nextRetryAt <= :now
        """)
    List<Job> findJobsReadyForRetry(
            @Param("status") JobStatus status,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT j
    FROM Job j
    WHERE j.status = :status
      AND j.scheduledAt <= :now
    """)
    List<Job> findDueScheduledJobs(
            @Param("status") JobStatus status,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("""
    UPDATE Job j
    SET j.status = :cancelledStatus,
        j.updatedAt = :now
    WHERE j.id = :jobId
      AND j.status IN :cancellableStatuses
    """)
    int cancelJob(
            @Param("jobId") UUID jobId,
            @Param("cancelledStatus") JobStatus cancelledStatus,
            @Param("cancellableStatuses") List<JobStatus> cancellableStatuses,
            @Param("now") LocalDateTime now
    );
}