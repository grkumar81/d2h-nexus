package org.nexus.d2h.retailer;

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
@RequestMapping("/api/v1/retailers")
public class RetailerController {

    private final RetailerService retailerService;

    public RetailerController(RetailerService retailerService) {
        this.retailerService = retailerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<RetailerDto>> create(@Valid @RequestBody CreateRetailerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(retailerService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER','FINANCE_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<RetailerDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(retailerService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<RetailerDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRetailerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(retailerService.update(id, request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER','FINANCE_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PageResponse<RetailerDto>>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) RetailerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "retailerName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(retailerService.search(query, status, pageable)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<RetailerDto>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(retailerService.activate(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<RetailerDto>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(retailerService.deactivate(id)));
    }
}
