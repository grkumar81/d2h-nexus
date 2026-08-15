package org.nexus.d2h.notification;

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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final NotificationConfigRepository configRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final TenantRepository tenantRepository;

    public NotificationService(NotificationConfigRepository configRepository,
                                NotificationDeliveryRepository deliveryRepository,
                                TenantRepository tenantRepository) {
        this.configRepository = configRepository;
        this.deliveryRepository = deliveryRepository;
        this.tenantRepository = tenantRepository;
    }

    // ── Config ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationConfigDto> listConfigs() {
        Long tenantId = resolveTenant().getId();
        return configRepository.findByTenantId(tenantId).stream()
                .map(NotificationConfigDto::from).toList();
    }

    @Transactional
    public NotificationConfigDto saveConfig(SaveNotificationConfigRequest request) {
        Tenant tenant = resolveTenant();
        NotificationConfig config = configRepository
                .findByTenantIdAndEventTypeAndChannel(tenant.getId(), request.eventType(), request.channel())
                .orElseGet(() -> {
                    NotificationConfig c = new NotificationConfig();
                    c.setTenant(tenant);
                    c.setEventType(request.eventType());
                    c.setChannel(request.channel());
                    c.setCreatedBy(currentUsername());
                    return c;
                });

        config.setEnabled(request.enabled());
        config.setRecipients(request.recipients());
        config.setUpdatedAt(Instant.now());
        NotificationConfig saved = configRepository.save(config);
        log.info("Notification config saved: event={} channel={} enabled={} tenant={}",
                request.eventType(), request.channel(), request.enabled(), tenant.getTenantCode());
        return NotificationConfigDto.from(saved);
    }

    @Transactional
    public void deleteConfig(Long id) {
        Tenant tenant = resolveTenant();
        NotificationConfig config = configRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NotificationConfig", id));
        configRepository.delete(config);
        log.info("Notification config deleted: id={} tenant={}", id, tenant.getTenantCode());
    }

    // ── Delivery history ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<NotificationDeliveryDto> listDeliveries(Pageable pageable) {
        Long tenantId = resolveTenant().getId();
        return PageResponse.from(
                deliveryRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                        .map(NotificationDeliveryDto::from));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Tenant resolveTenant() {
        String code = TenantContext.getCurrentTenant();
        if (code == null || code.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
        return tenantRepository.findByTenantCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", code));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
