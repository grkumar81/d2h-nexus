package org.nexus.d2h.dashboard;

import org.junit.jupiter.api.Test;
import org.nexus.d2h.auth.JwtProperties;
import org.nexus.d2h.auth.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DashboardService dashboardService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getDashboard_returns200WithKpis() throws Exception {
        when(dashboardService.getDashboard(any())).thenReturn(sampleDashboard());

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBoxSales").value(100000))
                .andExpect(jsonPath("$.data.totalReceived").value(70000))
                .andExpect(jsonPath("$.data.totalOutstanding").value(30000))
                .andExpect(jsonPath("$.data.totalAssets").value(10))
                .andExpect(jsonPath("$.data.totalRetailers").value(20));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getDashboard_withFyYear_returns200() throws Exception {
        when(dashboardService.getDashboard(2023)).thenReturn(sampleDashboard());

        mockMvc.perform(get("/api/v1/dashboard").param("fyYear", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.financialYearStart").value(2025));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getDashboard_monthlyTrendIncluded() throws Exception {
        DashboardDto dto = sampleDashboardWithTrend();
        when(dashboardService.getDashboard(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyTrend").isArray())
                .andExpect(jsonPath("$.data.monthlyTrend[0].month").value(4));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void getDashboard_topRetailersIncluded() throws Exception {
        when(dashboardService.getDashboard(any())).thenReturn(sampleDashboardWithTopRetailers());

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topByReceived[0].retailerCode").value("RET001"))
                .andExpect(jsonPath("$.data.topByOutstanding[0].retailerCode").value("RET002"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private DashboardDto sampleDashboard() {
        return new DashboardDto(
                BigDecimal.valueOf(100000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(30000), BigDecimal.valueOf(20000), 50L,
                10L, 4L, 3L, 2L, 1L, 0L, 0L, 0L,
                20L, 15L, 5L, 3L,
                List.of(), List.of(), List.of(),
                2025, 2026
        );
    }

    private DashboardDto sampleDashboardWithTrend() {
        return new DashboardDto(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L,
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L,
                List.of(new MonthlyTrendDto(2025, 4, BigDecimal.valueOf(10000),
                        BigDecimal.valueOf(8000), BigDecimal.valueOf(2000), BigDecimal.valueOf(2000))),
                List.of(), List.of(),
                2025, 2026
        );
    }

    private DashboardDto sampleDashboardWithTopRetailers() {
        return new DashboardDto(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L,
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L,
                List.of(),
                List.of(new TopRetailerDto(1L, "RET001", "Retailer One", BigDecimal.valueOf(50000))),
                List.of(new TopRetailerDto(2L, "RET002", "Retailer Two", BigDecimal.valueOf(15000))),
                2025, 2026
        );
    }
}
