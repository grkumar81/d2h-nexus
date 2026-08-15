package org.nexus.d2h.dashboard;

import org.nexus.d2h.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<DashboardDto>> getDashboard(
            @RequestParam(required = false) Integer fyYear) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(fyYear)));
    }
}
