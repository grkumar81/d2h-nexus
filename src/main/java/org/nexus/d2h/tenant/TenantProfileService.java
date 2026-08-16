package org.nexus.d2h.tenant;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TenantProfileService {

    private final TenantRepository tenantRepository;

    public TenantProfileService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public TenantProfileDto getProfile() {
        return TenantProfileDto.from(resolveTenant());
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusDto getSubscriptionStatus() {
        Tenant tenant = resolveTenant();
        return SubscriptionStatusDto.from(SubscriptionState.compute(tenant));
    }

    @Transactional
    public TenantProfileDto updateProfile(UpdateTenantProfileRequest request) {
        Tenant tenant = resolveTenant();
        tenant.setName(request.name().trim());
        tenant.setEmail(request.email());
        tenant.setPhone(request.phone());
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant profile updated: code={} by={}", saved.getTenantCode(), currentUsername());
        return TenantProfileDto.from(saved);
    }

    private Tenant resolveTenant() {
        String tenantCode = TenantContext.getCurrentTenant();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
        return tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantCode));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
