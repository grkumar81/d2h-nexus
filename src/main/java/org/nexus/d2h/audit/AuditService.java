package org.nexus.d2h.audit;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
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
    private final TenantRepository tenantRepository;

    public AuditService(AuditLogRepository auditLogRepository, TenantRepository tenantRepository) {
        this.auditLogRepository = auditLogRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long tenantId, String entityType, String entityId,
                       String action, String details, String ipAddress) {
        try {
            String performedBy = currentUsername();
            AuditLog entry = new AuditLog(tenantId, entityType, entityId, action, performedBy, details, ipAddress);
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
        Long tenantId = resolveTenantId();
        return PageResponse.from(
                auditLogRepository.search(tenantId, entityType, entityId, action, performedBy, from, to, pageable)
                        .map(AuditLogDto::from)
        );
    }

    private Long resolveTenantId() {
        String tenantCode = TenantContext.getCurrentTenant();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantCode));
        return tenant.getId();
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
