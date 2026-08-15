package org.nexus.d2h.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.tenantId = :tenantId
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId   IS NULL OR a.entityId   = :entityId)
              AND (:action     IS NULL OR a.action     = :action)
              AND (:performedBy IS NULL OR a.performedBy = :performedBy)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to   IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(@Param("tenantId") Long tenantId,
                          @Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          @Param("action") String action,
                          @Param("performedBy") String performedBy,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);
}
