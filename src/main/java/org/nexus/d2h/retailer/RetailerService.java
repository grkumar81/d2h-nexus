package org.nexus.d2h.retailer;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RetailerService {

    private final RetailerRepository retailerRepository;
    private final TenantRepository tenantRepository;

    public RetailerService(RetailerRepository retailerRepository, TenantRepository tenantRepository) {
        this.retailerRepository = retailerRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public RetailerDto create(CreateRetailerRequest request) {
        Tenant tenant = resolveTenant();

        if (retailerRepository.existsByTenantIdAndRetailerCode(tenant.getId(), request.retailerCode())) {
            throw new BusinessException("DUPLICATE_RETAILER_CODE",
                    "Retailer code '" + request.retailerCode() + "' already exists");
        }

        Retailer retailer = new Retailer();
        retailer.setTenant(tenant);
        applyCreate(retailer, request);

        Retailer saved = retailerRepository.save(retailer);
        log.info("Retailer created: code={} tenant={}", saved.getRetailerCode(), tenant.getTenantCode());
        return RetailerDto.from(saved);
    }

    @Transactional(readOnly = true)
    public RetailerDto getById(Long id) {
        return RetailerDto.from(findForCurrentTenant(id));
    }

    @Transactional
    public RetailerDto update(Long id, UpdateRetailerRequest request) {
        Retailer retailer = findForCurrentTenant(id);
        applyUpdate(retailer, request);
        retailer.setUpdatedBy(currentUsername());
        return RetailerDto.from(retailerRepository.save(retailer));
    }

    @Transactional
    public RetailerDto activate(Long id) {
        return changeStatus(id, RetailerStatus.ACTIVE);
    }

    @Transactional
    public RetailerDto deactivate(Long id) {
        return changeStatus(id, RetailerStatus.INACTIVE);
    }

    @Transactional(readOnly = true)
    public PageResponse<RetailerDto> search(String query, RetailerStatus status, Pageable pageable) {
        Long tenantId = resolveTenant().getId();
        Page<RetailerDto> page = retailerRepository
                .findAll(RetailerSpecification.search(tenantId, query, status), pageable)
                .map(RetailerDto::from);
        return PageResponse.from(page);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Retailer findForCurrentTenant(Long id) {
        Long tenantId = resolveTenant().getId();
        return retailerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", id));
    }

    private RetailerDto changeStatus(Long id, RetailerStatus newStatus) {
        Retailer retailer = findForCurrentTenant(id);
        retailer.setStatus(newStatus);
        retailer.setUpdatedBy(currentUsername());
        return RetailerDto.from(retailerRepository.save(retailer));
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

    private void applyCreate(Retailer r, CreateRetailerRequest req) {
        r.setRetailerCode(req.retailerCode().trim().toUpperCase());
        r.setRetailerName(req.retailerName().trim());
        r.setMobile(req.mobile());
        r.setAlternateMobile(req.alternateMobile());
        r.setEmail(req.email());
        r.setAddress(req.address());
        r.setCity(req.city());
        r.setState(req.state());
        r.setPinCode(req.pinCode());
        r.setGstNumber(req.gstNumber());
        r.setPanNumber(req.panNumber());
        r.setJoiningDate(req.joiningDate());
        r.setStatus(RetailerStatus.ACTIVE);
    }

    private void applyUpdate(Retailer r, UpdateRetailerRequest req) {
        r.setRetailerName(req.retailerName().trim());
        r.setMobile(req.mobile());
        r.setAlternateMobile(req.alternateMobile());
        r.setEmail(req.email());
        r.setAddress(req.address());
        r.setCity(req.city());
        r.setState(req.state());
        r.setPinCode(req.pinCode());
        r.setGstNumber(req.gstNumber());
        r.setPanNumber(req.panNumber());
        r.setJoiningDate(req.joiningDate());
    }
}
