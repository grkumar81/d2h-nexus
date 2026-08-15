package org.nexus.d2h.report;

import org.nexus.d2h.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService exportService;

    public ReportController(ReportService reportService, ReportExportService exportService) {
        this.reportService = reportService;
        this.exportService = exportService;
    }

    @GetMapping("/retailers")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<List<RetailerReportDto>>> allRetailerReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.allRetailerReport(dateFrom, dateTo)));
    }

    @GetMapping("/retailers/{retailerId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<RetailerReportDto>> retailerReport(
            @PathVariable Long retailerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.retailerReport(retailerId, dateFrom, dateTo)));
    }

    @GetMapping("/period")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PeriodReportDto>> periodReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.periodReport(dateFrom, dateTo)));
    }

    // ── CSV exports ───────────────────────────────────────────────────────────

    @GetMapping("/retailers/export/csv")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<byte[]> exportRetailersCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] data = exportService.retailerReportCsv(reportService.allRetailerReport(dateFrom, dateTo));
        return download(data, "retailer-report.csv", "text/csv");
    }

    @GetMapping("/period/export/csv")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<byte[]> exportPeriodCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] data = exportService.periodReportCsv(reportService.periodReport(dateFrom, dateTo));
        return download(data, "period-report.csv", "text/csv");
    }

    // ── Excel exports ─────────────────────────────────────────────────────────

    @GetMapping("/retailers/export/excel")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<byte[]> exportRetailersExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] data = exportService.retailerReportExcel(reportService.allRetailerReport(dateFrom, dateTo));
        return download(data, "retailer-report.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/period/export/excel")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<byte[]> exportPeriodExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] data = exportService.periodReportExcel(reportService.periodReport(dateFrom, dateTo));
        return download(data, "period-report.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> download(byte[] data, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(data.length)
                .body(data);
    }
}
