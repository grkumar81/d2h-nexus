package org.nexus.d2h.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status IN (
                org.nexus.d2h.notification.NotificationStatus.PENDING,
                org.nexus.d2h.notification.NotificationStatus.RETRYING
            )
            AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findDueForProcessing(@Param("now") Instant now);
}
