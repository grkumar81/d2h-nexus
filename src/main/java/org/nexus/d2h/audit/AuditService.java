package org.nexus.d2h.audit;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String entityType, String entityId,
                       String action, String details, String ipAddress) {
        try {
            String performedBy = currentUsername();
            AuditLog entry = new AuditLog(entityType, entityId, action, performedBy, details, ipAddress);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit log: entity={}/{} action={} error={}",
                    entityType, entityId, action, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> search(String entityType, String entityId,
                                             String action, String performedBy,
                                             Instant from, Instant to, Pageable pageable) {
        return PageResponse.from(
                auditLogRepository.search(entityType, entityId, action, performedBy, from, to, pageable)
                        .map(AuditLogDto::from)
        );
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
