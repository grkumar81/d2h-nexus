package org.nexus.d2h.tenant;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PlatformTenantService {

    private final TenantRepository tenantRepository;
    private final TenantSchemaService tenantSchemaService;

    public PlatformTenantService(TenantRepository tenantRepository,
                                 TenantSchemaService tenantSchemaService) {
        this.tenantRepository = tenantRepository;
        this.tenantSchemaService = tenantSchemaService;
    }

    @Transactional
    public PlatformTenantDto create(CreateTenantRequest request) {
        String code = request.tenantCode().toUpperCase().trim();
        if (tenantRepository.findByTenantCode(code).isPresent()) {
            throw new BusinessException("DUPLICATE_TENANT_CODE",
                    "Tenant code '" + code + "' already exists");
        }
        Tenant tenant = new Tenant();
        tenant.setTenantCode(code);
        tenant.setName(request.name().trim());
        tenant.setEmail(request.email());
        tenant.setPhone(request.phone());
        tenant.setSchemaName("d2h_tenant_" + code.toLowerCase());
        tenant.setStatus(TenantStatus.PENDING);

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created: code={} schema={} by={}", saved.getTenantCode(),
                saved.getSchemaName(), currentUsername());
        return PlatformTenantDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformTenantDto> list(Pageable pageable) {
        return PageResponse.from(tenantRepository.findAll(pageable).map(PlatformTenantDto::from));
    }

    @Transactional(readOnly = true)
    public PlatformTenantDto get(Long id) {
        return PlatformTenantDto.from(findById(id));
    }

    @Transactional
    public PlatformTenantDto update(Long id, UpdateTenantRequest request) {
        Tenant tenant = findById(id);
        tenant.setName(request.name().trim());
        tenant.setEmail(request.email());
        tenant.setPhone(request.phone());
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant updated: id={} code={} by={}", saved.getId(),
                saved.getTenantCode(), currentUsername());
        return PlatformTenantDto.from(saved);
    }

    /**
     * Transitions tenant to APPROVED and provisions its schema.
     * Idempotent — re-approving an already-APPROVED tenant re-runs schema provisioning
     * (CREATE SCHEMA IF NOT EXISTS is safe).
     * Cannot approve a SUSPENDED or DEACTIVATED tenant.
     */
    @Transactional
    public PlatformTenantDto approve(Long id) {
        Tenant tenant = findById(id);
        if (tenant.getStatus() == TenantStatus.SUSPENDED
                || tenant.getStatus() == TenantStatus.DEACTIVATED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "Cannot approve a tenant with status: " + tenant.getStatus());
        }
        tenant.setStatus(TenantStatus.APPROVED);
        tenantRepository.save(tenant);

        // Provision schema — runs T1-T6 scripts; throws BusinessException on failure
        tenantSchemaService.provisionSchema(tenant.getId());

        // Transition to ACTIVE after successful provisioning
        tenant.setStatus(TenantStatus.ACTIVE);
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant approved and activated: id={} code={} schema={} by={}",
                saved.getId(), saved.getTenantCode(), saved.getSchemaName(), currentUsername());
        return PlatformTenantDto.from(saved);
    }

    @Transactional
    public PlatformTenantDto suspend(Long id) {
        Tenant tenant = findById(id);
        if (tenant.getStatus() == TenantStatus.DEACTIVATED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "Cannot suspend a deactivated tenant");
        }
        tenant.setStatus(TenantStatus.SUSPENDED);
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant suspended: id={} code={} by={}", saved.getId(),
                saved.getTenantCode(), currentUsername());
        return PlatformTenantDto.from(saved);
    }

    @Transactional
    public PlatformTenantDto deactivate(Long id) {
        Tenant tenant = findById(id);
        tenant.setStatus(TenantStatus.DEACTIVATED);
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant deactivated: id={} code={} by={}", saved.getId(),
                saved.getTenantCode(), currentUsername());
        return PlatformTenantDto.from(saved);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    public Tenant findTenantById(Long id) {
        return findById(id);
    }

    private Tenant findById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
