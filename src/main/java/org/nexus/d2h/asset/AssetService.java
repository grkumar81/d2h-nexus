package org.nexus.d2h.asset;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Service
public class AssetService {

    // Statuses that block assignment/tagging
    private static final Set<AssetStatus> UNASSIGNABLE =
            Set.of(AssetStatus.SOLD, AssetStatus.DAMAGED, AssetStatus.LOST,
                   AssetStatus.BLOCKED, AssetStatus.SCRAPPED);

    // Statuses that allow a box sale
    static final Set<AssetStatus> SALEABLE =
            Set.of(AssetStatus.AVAILABLE, AssetStatus.ALLOCATED);

    private final AssetRepository assetRepository;
    private final AssetHistoryRepository historyRepository;
    private final TenantRepository tenantRepository;
    private final RetailerRepository retailerRepository;

    public AssetService(AssetRepository assetRepository,
                        AssetHistoryRepository historyRepository,
                        TenantRepository tenantRepository,
                        RetailerRepository retailerRepository) {
        this.assetRepository = assetRepository;
        this.historyRepository = historyRepository;
        this.tenantRepository = tenantRepository;
        this.retailerRepository = retailerRepository;
    }

    @Transactional
    public AssetDto create(CreateAssetRequest request) {
        Tenant tenant = resolveTenant();
        if (assetRepository.existsByTenantIdAndSerialNumber(tenant.getId(), request.serialNumber())) {
            throw new BusinessException("DUPLICATE_SERIAL_NUMBER",
                    "Serial number '" + request.serialNumber() + "' already exists");
        }
        StbAsset asset = new StbAsset();
        asset.setTenantId(tenant.getId());
        asset.setSerialNumber(request.serialNumber().trim().toUpperCase());
        asset.setBoxNumber(request.boxNumber());
        asset.setModel(request.model());
        asset.setManufacturer(request.manufacturer());
        asset.setBatch(request.batch());
        asset.setPurchaseDate(request.purchaseDate());
        asset.setPurchaseCost(request.purchaseCost());
        asset.setStatus(AssetStatus.AVAILABLE);

        StbAsset saved = assetRepository.save(asset);
        recordHistory(saved, null, AssetStatus.AVAILABLE, null, "Asset created");
        log.info("Asset created: serial={} tenant={}", saved.getSerialNumber(), tenant.getTenantCode());
        return AssetDto.from(saved);
    }

    @Transactional(readOnly = true)
    public AssetDto getById(Long id) {
        return AssetDto.from(findForCurrentTenant(id));
    }

    @Transactional
    public AssetDto update(Long id, UpdateAssetRequest request) {
        StbAsset asset = findForCurrentTenant(id);
        asset.setBoxNumber(request.boxNumber());
        asset.setModel(request.model());
        asset.setManufacturer(request.manufacturer());
        asset.setBatch(request.batch());
        asset.setPurchaseDate(request.purchaseDate());
        asset.setPurchaseCost(request.purchaseCost());
        asset.setUpdatedBy(currentUsername());
        return AssetDto.from(assetRepository.save(asset));
    }

    @Transactional
    public AssetDto tag(Long id, TagAssetRequest request) {
        StbAsset asset = findForCurrentTenant(id);
        if (UNASSIGNABLE.contains(asset.getStatus())) {
            throw new BusinessException("ASSET_NOT_TAGGABLE",
                    "Asset in status " + asset.getStatus() + " cannot be tagged");
        }
        Retailer retailer = findRetailerForCurrentTenant(request.retailerId());
        AssetStatus from = asset.getStatus();

        asset.setRetailer(retailer);
        asset.setTaggingDate(request.taggingDate() != null ? request.taggingDate() : LocalDate.now());
        asset.setStatus(AssetStatus.ALLOCATED);
        asset.setUpdatedBy(currentUsername());

        StbAsset saved = assetRepository.save(asset);
        recordHistory(saved, from, AssetStatus.ALLOCATED, retailer,
                request.remarks() != null ? request.remarks() : "Tagged to retailer");
        return AssetDto.from(saved);
    }

    @Transactional
    public AssetDto transition(Long id, AssetStatus newStatus, String remarks) {
        StbAsset asset = findForCurrentTenant(id);
        validateTransition(asset.getStatus(), newStatus);
        AssetStatus from = asset.getStatus();
        asset.setStatus(newStatus);
        asset.setUpdatedBy(currentUsername());
        if (newStatus == AssetStatus.RETURNED) {
            asset.setReturnDate(LocalDate.now());
        }
        StbAsset saved = assetRepository.save(asset);
        recordHistory(saved, from, newStatus, saved.getRetailer(), remarks);
        return AssetDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetDto> search(String query, AssetStatus status, Long retailerId, Pageable pageable) {
        Long tenantId = resolveTenant().getId();
        Page<AssetDto> page = assetRepository
                .findAll(AssetSpecification.search(tenantId, query, status, retailerId), pageable)
                .map(AssetDto::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetHistoryDto> getHistory(Long assetId, Pageable pageable) {
        Long tenantId = resolveTenant().getId();
        // Verify asset belongs to tenant
        findForCurrentTenant(assetId);
        Page<AssetHistoryDto> page = historyRepository
                .findByAssetIdAndTenantIdOrderByChangedAtDesc(assetId, tenantId, pageable)
                .map(AssetHistoryDto::from);
        return PageResponse.from(page);
    }

    // ── package-private: used by BoxSaleService in same transaction ───────────

    @Transactional
    public StbAsset markSold(Long assetId, Long tenantId, LocalDate saleDate, String changedBy) {
        StbAsset asset = assetRepository.findByIdAndTenantId(assetId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));
        if (!SALEABLE.contains(asset.getStatus())) {
            throw new BusinessException("ASSET_NOT_SALEABLE",
                    "Asset " + asset.getSerialNumber() + " is not available for sale (status=" + asset.getStatus() + ")");
        }
        AssetStatus from = asset.getStatus();
        asset.setStatus(AssetStatus.SOLD);
        asset.setSaleDate(saleDate);
        asset.setUpdatedBy(changedBy);
        StbAsset saved = assetRepository.save(asset);
        recordHistory(saved, from, AssetStatus.SOLD, saved.getRetailer(), "Box sale");
        return saved;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private StbAsset findForCurrentTenant(Long id) {
        Long tenantId = resolveTenant().getId();
        return assetRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
    }

    private Retailer findRetailerForCurrentTenant(Long retailerId) {
        Long tenantId = resolveTenant().getId();
        return retailerRepository.findByIdAndTenantId(retailerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));
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

    private void recordHistory(StbAsset asset, AssetStatus from, AssetStatus to,
                               Retailer retailer, String remarks) {
        historyRepository.save(new StbAssetHistory(asset, from, to, retailer, currentUsername(), remarks));
    }

    private void validateTransition(AssetStatus current, AssetStatus target) {
        // Finalized states cannot transition further
        if (current == AssetStatus.SCRAPPED) {
            throw new BusinessException("INVALID_ASSET_TRANSITION",
                    "Scrapped asset cannot change status");
        }
        // SOLD assets can only be ACTIVATED or RETURNED
        if (current == AssetStatus.SOLD &&
                target != AssetStatus.ACTIVATED && target != AssetStatus.RETURNED) {
            throw new BusinessException("INVALID_ASSET_TRANSITION",
                    "Sold asset can only be ACTIVATED or RETURNED");
        }
    }
}
