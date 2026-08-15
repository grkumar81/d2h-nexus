package org.nexus.d2h.audit;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        String entityType,
        String entityId,
        String action,
        String performedBy,
        String details,
        String ipAddress,
        Instant createdAt
) {
    static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getPerformedBy(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
