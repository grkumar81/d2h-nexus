package org.nexus.d2h.report;

import org.junit.jupiter.api.Test;
import org.nexus.d2h.auth.JwtProperties;
import org.nexus.d2h.auth.JwtService;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReportService reportService;
    @MockitoBean ReportExportService exportService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void allRetailerReport_returns200() throws Exception {
        when(reportService.allRetailerReport(any(), any())).thenReturn(List.of(sampleRetailerReport()));

        mockMvc.perform(get("/api/v1/reports/retailers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].retailerCode").value("RET001"))
                .andExpect(jsonPath("$.data[0].outstanding").value(30000));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void allRetailerReport_withDateRange_returns200() throws Exception {
        when(reportService.allRetailerReport(any(), any())).thenReturn(List.of(sampleRetailerReport()));

        mockMvc.perform(get("/api/v1/reports/retailers")
                        .param("dateFrom", "2025-04-01")
                        .param("dateTo", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void retailerReport_returns200() throws Exception {
        when(reportService.retailerReport(eq(5L), any(), any())).thenReturn(sampleRetailerReport());

        mockMvc.perform(get("/api/v1/reports/retailers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retailerCode").value("RET001"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void retailerReport_notFound_returns404() throws Exception {
        when(reportService.retailerReport(eq(99L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Retailer", 99L));

        mockMvc.perform(get("/api/v1/reports/retailers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void periodReport_returns200() throws Exception {
        when(reportService.periodReport(any(), any())).thenReturn(samplePeriodReport());

        mockMvc.perform(get("/api/v1/reports/period"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boxSales").value(200000))
                .andExpect(jsonPath("$.data.transactionCount").value(100));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void exportRetailersCsv_returns200WithAttachment() throws Exception {
        when(reportService.allRetailerReport(any(), any())).thenReturn(List.of(sampleRetailerReport()));
        when(exportService.retailerReportCsv(any())).thenReturn("code,name\nRET001,Test".getBytes());

        mockMvc.perform(get("/api/v1/reports/retailers/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"retailer-report.csv\""));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void exportRetailersExcel_returns200WithAttachment() throws Exception {
        when(reportService.allRetailerReport(any(), any())).thenReturn(List.of(sampleRetailerReport()));
        when(exportService.retailerReportExcel(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/reports/retailers/export/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"retailer-report.xlsx\""));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void exportPeriodCsv_returns200WithAttachment() throws Exception {
        when(reportService.periodReport(any(), any())).thenReturn(samplePeriodReport());
        when(exportService.periodReportCsv(any())).thenReturn("dateFrom,dateTo\n2025-04-01,2026-03-31".getBytes());

        mockMvc.perform(get("/api/v1/reports/period/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"period-report.csv\""));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RetailerReportDto sampleRetailerReport() {
        return new RetailerReportDto(5L, "RET001", "Test Retailer",
                BigDecimal.valueOf(100000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(30000), BigDecimal.valueOf(5000));
    }

    private PeriodReportDto samplePeriodReport() {
        return new PeriodReportDto(
                LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(200000), BigDecimal.valueOf(150000),
                BigDecimal.valueOf(50000), BigDecimal.valueOf(30000), 100L);
    }
}
