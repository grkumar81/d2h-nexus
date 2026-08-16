package org.nexus.d2h.usermgmt;

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
@RequestMapping("/api/v1/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(userManagementService.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("username").ascending());
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.listForTenant(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.activate(id)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.deactivate(id)));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id) {
        userManagementService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully"));
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userManagementService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }
}
