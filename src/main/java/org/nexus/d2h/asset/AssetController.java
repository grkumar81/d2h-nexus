package org.nexus.d2h.asset;

import jakarta.validation.Valid;
import org.nexus.d2h.common.ApiResponse;
import org.nexus.d2h.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<AssetDto>> create(@Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(assetService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER','FINANCE_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<AssetDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<AssetDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssetRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.update(id, request)));
    }

    @PatchMapping("/{id}/tag")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<AssetDto>> tag(
            @PathVariable Long id,
            @Valid @RequestBody TagAssetRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.tag(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<AssetDto>> changeStatus(
            @PathVariable Long id,
            @RequestParam AssetStatus status,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.transition(id, status, remarks)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER','FINANCE_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PageResponse<AssetDto>>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Long retailerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "serialNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(assetService.search(query, status, retailerId, pageable)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER','FINANCE_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PageResponse<AssetHistoryDto>>> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.ok(assetService.getHistory(id, pageable)));
    }
}
