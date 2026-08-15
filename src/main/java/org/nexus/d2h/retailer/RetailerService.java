package org.nexus.d2h.retailer;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.audit.AuditService;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RetailerService {

    private final RetailerRepository retailerRepository;
    private final AuditService auditService;

    public RetailerService(RetailerRepository retailerRepository, AuditService auditService) {
        this.retailerRepository = retailerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RetailerDto create(CreateRetailerRequest request) {
        String code = request.retailerCode().trim().toUpperCase();
        if (retailerRepository.existsByRetailerCode(code)) {
            throw new BusinessException("DUPLICATE_RETAILER_CODE",
                    "Retailer code '" + code + "' already exists");
        }
        Retailer retailer = new Retailer();
        applyCreate(retailer, request, code);
        Retailer saved = retailerRepository.save(retailer);
        log.info("Retailer created: code={}", saved.getRetailerCode());
        auditService.record("Retailer", String.valueOf(saved.getId()), "CREATE",
                "code=" + saved.getRetailerCode(), null);
        return RetailerDto.from(saved);
    }

    @Transactional(readOnly = true)
    public RetailerDto getById(Long id) {
        return RetailerDto.from(findById(id));
    }

    @Transactional
    public RetailerDto update(Long id, UpdateRetailerRequest request) {
        Retailer retailer = findById(id);
        applyUpdate(retailer, request);
        retailer.setUpdatedBy(currentUsername());
        Retailer saved = retailerRepository.save(retailer);
        auditService.record("Retailer", String.valueOf(id), "UPDATE",
                "code=" + saved.getRetailerCode(), null);
        return RetailerDto.from(saved);
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
        Page<RetailerDto> page = retailerRepository
                .findAll(RetailerSpecification.search(query, status), pageable)
                .map(RetailerDto::from);
        return PageResponse.from(page);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Retailer findById(Long id) {
        return retailerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", id));
    }

    private RetailerDto changeStatus(Long id, RetailerStatus newStatus) {
        Retailer retailer = findById(id);
        retailer.setStatus(newStatus);
        retailer.setUpdatedBy(currentUsername());
        return RetailerDto.from(retailerRepository.save(retailer));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private void applyCreate(Retailer r, CreateRetailerRequest req, String code) {
        r.setRetailerCode(code);
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
