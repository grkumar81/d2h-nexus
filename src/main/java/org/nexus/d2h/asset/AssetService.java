package org.nexus.d2h.asset;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
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

    private static final Set<AssetStatus> UNASSIGNABLE =
            Set.of(AssetStatus.SOLD, AssetStatus.DAMAGED, AssetStatus.LOST,
                   AssetStatus.BLOCKED, AssetStatus.SCRAPPED);

    static final Set<AssetStatus> SALEABLE =
            Set.of(AssetStatus.AVAILABLE, AssetStatus.ALLOCATED);

    private final AssetRepository assetRepository;
    private final AssetHistoryRepository historyRepository;
    private final RetailerRepository retailerRepository;

    public AssetService(AssetRepository assetRepository,
                        AssetHistoryRepository historyRepository,
                        RetailerRepository retailerRepository) {
        this.assetRepository = assetRepository;
        this.historyRepository = historyRepository;
        this.retailerRepository = retailerRepository;
    }

    @Transactional
    public AssetDto create(CreateAssetRequest request) {
        if (assetRepository.existsBySerialNumber(request.serialNumber())) {
            throw new BusinessException("DUPLICATE_SERIAL_NUMBER",
                    "Serial number '" + request.serialNumber() + "' already exists");
        }
        StbAsset asset = new StbAsset();
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
        log.info("Asset created: serial={}", saved.getSerialNumber());
        return AssetDto.from(saved);
    }

    @Transactional(readOnly = true)
    public AssetDto getById(Long id) {
        return AssetDto.from(findById(id));
    }

    @Transactional
    public AssetDto update(Long id, UpdateAssetRequest request) {
        StbAsset asset = findById(id);
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
        StbAsset asset = findById(id);
        if (UNASSIGNABLE.contains(asset.getStatus())) {
            throw new BusinessException("ASSET_NOT_TAGGABLE",
                    "Asset in status " + asset.getStatus() + " cannot be tagged");
        }
        Retailer retailer = retailerRepository.findById(request.retailerId())
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", request.retailerId()));
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
        StbAsset asset = findById(id);
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
        Page<AssetDto> page = assetRepository
                .findAll(AssetSpecification.search(query, status, retailerId), pageable)
                .map(AssetDto::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetHistoryDto> getHistory(Long assetId, Pageable pageable) {
        findById(assetId); // verify exists
        Page<AssetHistoryDto> page = historyRepository
                .findByAssetIdOrderByChangedAtDesc(assetId, pageable)
                .map(AssetHistoryDto::from);
        return PageResponse.from(page);
    }

    @Transactional
    public StbAsset markSold(Long assetId, LocalDate saleDate, String changedBy) {
        StbAsset asset = assetRepository.findById(assetId)
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

    private StbAsset findById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
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
        if (current == AssetStatus.SCRAPPED) {
            throw new BusinessException("INVALID_ASSET_TRANSITION", "Scrapped asset cannot change status");
        }
        if (current == AssetStatus.SOLD &&
                target != AssetStatus.ACTIVATED && target != AssetStatus.RETURNED) {
            throw new BusinessException("INVALID_ASSET_TRANSITION",
                    "Sold asset can only be ACTIVATED or RETURNED");
        }
    }
}
