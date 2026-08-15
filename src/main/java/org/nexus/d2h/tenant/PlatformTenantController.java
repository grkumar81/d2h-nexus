package org.nexus.d2h.tenant;

import jakarta.validation.Valid;
import org.nexus.d2h.common.ApiResponse;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.tenant.DataMigrationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/platform/tenants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformTenantController {

    private final PlatformTenantService service;
    private final DataMigrationService dataMigrationService;

    public PlatformTenantController(PlatformTenantService service,
                                     DataMigrationService dataMigrationService) {
        this.service = service;
        this.dataMigrationService = dataMigrationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlatformTenantDto>> create(
            @Valid @RequestBody CreateTenantRequest request) {
        PlatformTenantDto dto = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/platform/tenants/" + dto.id()))
                .body(ApiResponse.created(dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlatformTenantDto>>> list(
            @PageableDefault(size = 20, sort = "tenantCode") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformTenantDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformTenantDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PlatformTenantDto>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.approve(id)));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<PlatformTenantDto>> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.suspend(id)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<PlatformTenantDto>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.deactivate(id)));
    }

    @PostMapping("/migrate")
    public ResponseEntity<ApiResponse<String>> migrateAll() {
        dataMigrationService.migrateAll();
        return ResponseEntity.ok(ApiResponse.ok("Migration completed"));
    }

    @PostMapping("/{id}/migrate")
    public ResponseEntity<ApiResponse<String>> migrateTenant(@PathVariable Long id) {
        dataMigrationService.migrateTenant(service.findTenantById(id));
        return ResponseEntity.ok(ApiResponse.ok("Migration completed for tenant " + id));
    }
}
