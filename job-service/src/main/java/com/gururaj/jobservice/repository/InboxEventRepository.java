package com.gururaj.jobservice.repository;

import com.gururaj.jobservice.entity.InboxEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class InboxEventRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public boolean insertIfNotExists(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            LocalDateTime receivedAt) {

        int inserted = entityManager.createNativeQuery("""
                INSERT INTO inbox_events
                    (event_id, event_type, aggregate_id, received_at)
                VALUES
                    (:eventId, :eventType, :aggregateId, :receivedAt)
                ON CONFLICT (event_id) DO NOTHING
                """)
                .setParameter("eventId", eventId)
                .setParameter("eventType", eventType)
                .setParameter("aggregateId", aggregateId)
                .setParameter("receivedAt", receivedAt)
                .executeUpdate();

        return inserted == 1;
    }
}