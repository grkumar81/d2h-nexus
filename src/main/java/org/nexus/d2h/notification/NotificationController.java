package org.nexus.d2h.notification;

import jakarta.validation.Valid;
import org.nexus.d2h.common.ApiResponse;
import org.nexus.d2h.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── Config ────────────────────────────────────────────────────────────────

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<List<NotificationConfigDto>>> listConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.listConfigs()));
    }

    @PostMapping("/config")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<NotificationConfigDto>> saveConfig(
            @Valid @RequestBody SaveNotificationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.saveConfig(request)));
    }

    @DeleteMapping("/config/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        notificationService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Configuration deleted"));
    }

    // ── Delivery history ──────────────────────────────────────────────────────

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDeliveryDto>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(notificationService.listDeliveries(pageable)));
    }
}
