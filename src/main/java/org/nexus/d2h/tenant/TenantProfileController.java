package org.nexus.d2h.tenant;

import jakarta.validation.Valid;
import org.nexus.d2h.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenant/profile")
public class TenantProfileController {

    private final TenantProfileService tenantProfileService;

    public TenantProfileController(TenantProfileService tenantProfileService) {
        this.tenantProfileService = tenantProfileService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TenantProfileDto>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(tenantProfileService.getProfile()));
    }

    @GetMapping("/subscription")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SubscriptionStatusDto>> getSubscription() {
        return ResponseEntity.ok(ApiResponse.ok(tenantProfileService.getSubscriptionStatus()));
    }

    @PutMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<TenantProfileDto>> updateProfile(
            @Valid @RequestBody UpdateTenantProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tenantProfileService.updateProfile(request)));
    }
}
