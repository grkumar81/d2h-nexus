package org.nexus.d2h.upload;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.asset.StbAsset;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.finance.PaymentMethod;
import org.nexus.d2h.notification.NotificationEventPublisher;
import org.nexus.d2h.notification.NotificationEventType;
import org.nexus.d2h.recharge.RechargeService;
import org.nexus.d2h.recharge.RechargeType;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class RechargeUploadService {

    private static final int BATCH_SIZE = 100;
    private static final List<String> REQUIRED_HEADERS = List.of(
            "retailer_code", "recharge_date", "amount", "recharge_type", "reference"
    );

    private final RechargeService rechargeService;
    private final RetailerRepository retailerRepository;
    private final AssetRepository assetRepository;
    private final TenantRepository tenantRepository;
    private final NotificationEventPublisher eventPublisher;

    public RechargeUploadService(RechargeService rechargeService,
                                  RetailerRepository retailerRepository,
                                  AssetRepository assetRepository,
                                  TenantRepository tenantRepository,
                                  NotificationEventPublisher eventPublisher) {
        this.rechargeService = rechargeService;
        this.retailerRepository = retailerRepository;
        this.assetRepository = assetRepository;
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RechargeUploadResult upload(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("EMPTY_FILE", "Uploaded file is empty");
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".csv")) return processCsv(file);
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) return processExcel(file);
        throw new BusinessException("UNSUPPORTED_FILE_TYPE", "Only CSV and Excel files are supported");
    }

    private RechargeUploadResult processCsv(MultipartFile file) {
        Tenant tenant = resolveTenant();
        List<UploadResult.RowError> errors = new ArrayList<>();
        Set<String> seenRefs = new HashSet<>();
        int[] counters = {0, 0, 0}; // total, success, duplicate
        BigDecimal[] amounts = {BigDecimal.ZERO};

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null) throw new BusinessException("EMPTY_FILE", "CSV file has no content");
            Map<String, Integer> idx = buildHeaderIndex(headers);
            validateRequiredHeaders(idx);

            String[] row;
            int rowNum = 1;
            while ((row = reader.readNext()) != null) {
                rowNum++;
                counters[0]++;
                String rowData = String.join(",", row);
                try {
                    RowData rd = parseRow(row, idx, rowNum, tenant, seenRefs);
                    seenRefs.add(rd.reference());
                    rechargeService.createFromUpload(tenant, rd.retailer(), rd.asset(),
                            rd.type(), rd.date(), rd.amount(), rd.paymentMethod(),
                            rd.reference(), rd.externalReference(), rd.paymentReference(),
                            rd.servicePeriod(), rd.description(), rd.remarks());
                    counters[1]++;
                    amounts[0] = amounts[0].add(rd.amount());
                } catch (BusinessException e) {
                    if ("DUPLICATE_REFERENCE".equals(e.getCode())) counters[2]++;
                    errors.add(new UploadResult.RowError(rowNum, rowData, e.getMessage()));
                }
            }
        } catch (IOException | CsvValidationException e) {
            throw new BusinessException("FILE_READ_ERROR", "Failed to read CSV: " + e.getMessage());
        }

        int failed = counters[0] - counters[1] - counters[2];
        log.info("Recharge CSV upload: total={} success={} failed={} dup={} tenant={}",
                counters[0], counters[1], failed, counters[2], tenant.getTenantCode());
        publishUploadEvent(tenant, counters[0], counters[1], failed, counters[2], amounts[0]);
        return new RechargeUploadResult(UUID.randomUUID().toString(), counters[0], counters[1],
                failed, counters[2], amounts[0], errors);
    }

    private RechargeUploadResult processExcel(MultipartFile file) {
        Tenant tenant = resolveTenant();
        List<UploadResult.RowError> errors = new ArrayList<>();
        Set<String> seenRefs = new HashSet<>();
        int[] counters = {0, 0, 0};
        BigDecimal[] amounts = {BigDecimal.ZERO};

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BusinessException("EMPTY_FILE", "Excel file has no content");
            Map<String, Integer> idx = buildExcelHeaderIndex(headerRow);
            validateRequiredHeaders(idx);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                counters[0]++;
                String rowData = "row " + (i + 1);
                try {
                    RowData rd = parseExcelRow(row, idx, i + 1, tenant, seenRefs);
                    seenRefs.add(rd.reference());
                    rechargeService.createFromUpload(tenant, rd.retailer(), rd.asset(),
                            rd.type(), rd.date(), rd.amount(), rd.paymentMethod(),
                            rd.reference(), rd.externalReference(), rd.paymentReference(),
                            rd.servicePeriod(), rd.description(), rd.remarks());
                    counters[1]++;
                    amounts[0] = amounts[0].add(rd.amount());
                } catch (BusinessException e) {
                    if ("DUPLICATE_REFERENCE".equals(e.getCode())) counters[2]++;
                    errors.add(new UploadResult.RowError(i + 1, rowData, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_ERROR", "Failed to read Excel: " + e.getMessage());
        }

        int failed = counters[0] - counters[1] - counters[2];
        log.info("Recharge Excel upload: total={} success={} failed={} dup={} tenant={}",
                counters[0], counters[1], failed, counters[2], tenant.getTenantCode());
        publishUploadEvent(tenant, counters[0], counters[1], failed, counters[2], amounts[0]);
        return new RechargeUploadResult(UUID.randomUUID().toString(), counters[0], counters[1],
                failed, counters[2], amounts[0], errors);
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private void publishUploadEvent(Tenant tenant, int total, int success, int failed,
                                     int duplicates, BigDecimal totalAmount) {
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("totalRows", total);
            payload.put("successCount", success);
            payload.put("failureCount", failed);
            payload.put("duplicateCount", duplicates);
            payload.put("totalAmount", totalAmount.toPlainString());
            eventPublisher.publish(tenant, NotificationEventType.RECHARGE_UPLOAD_COMPLETED,
                    "upload", payload);
        } catch (Exception e) {
            log.warn("Failed to publish recharge upload notification: {}", e.getMessage());
        }
    }

    // ── Row parsing ───────────────────────────────────────────────────────────

    private RowData parseRow(String[] row, Map<String, Integer> idx, int rowNum,
                              Tenant tenant, Set<String> seenRefs) {
        String retailerCode = require(cell(row, idx, "retailer_code"), "retailer_code", rowNum);
        String dateStr      = require(cell(row, idx, "recharge_date"), "recharge_date", rowNum);
        String amountStr    = require(cell(row, idx, "amount"), "amount", rowNum);
        String typeStr      = require(cell(row, idx, "recharge_type"), "recharge_type", rowNum);
        String reference    = require(cell(row, idx, "reference"), "reference", rowNum);

        Retailer retailer = findRetailer(tenant, retailerCode, rowNum);
        LocalDate date    = parseDate(dateStr, rowNum);
        BigDecimal amount = parseAmount(amountStr, rowNum);
        RechargeType type = parseRechargeType(typeStr, rowNum);
        PaymentMethod pm  = parsePaymentMethod(optCell(row, idx, "payment_method"));
        StbAsset asset    = findAsset(tenant, optCell(row, idx, "stb_serial"), rowNum);

        if (seenRefs.contains(reference))
            throw new BusinessException("DUPLICATE_REFERENCE", "Duplicate reference in file: " + reference);

        return new RowData(retailer, asset, type, date, amount, pm, reference,
                optCell(row, idx, "external_reference"),
                optCell(row, idx, "payment_reference"),
                optCell(row, idx, "service_period"),
                optCell(row, idx, "description"),
                optCell(row, idx, "remarks"));
    }

    private RowData parseExcelRow(Row row, Map<String, Integer> idx, int rowNum,
                                   Tenant tenant, Set<String> seenRefs) {
        String retailerCode = require(excelCell(row, idx, "retailer_code"), "retailer_code", rowNum);
        String dateStr      = require(excelCell(row, idx, "recharge_date"), "recharge_date", rowNum);
        String amountStr    = require(excelCell(row, idx, "amount"), "amount", rowNum);
        String typeStr      = require(excelCell(row, idx, "recharge_type"), "recharge_type", rowNum);
        String reference    = require(excelCell(row, idx, "reference"), "reference", rowNum);

        Retailer retailer = findRetailer(tenant, retailerCode, rowNum);
        LocalDate date    = parseDate(dateStr, rowNum);
        BigDecimal amount = parseAmount(amountStr, rowNum);
        RechargeType type = parseRechargeType(typeStr, rowNum);
        PaymentMethod pm  = parsePaymentMethod(optExcelCell(row, idx, "payment_method"));
        StbAsset asset    = findAsset(tenant, optExcelCell(row, idx, "stb_serial"), rowNum);

        if (seenRefs.contains(reference))
            throw new BusinessException("DUPLICATE_REFERENCE", "Duplicate reference in file: " + reference);

        return new RowData(retailer, asset, type, date, amount, pm, reference,
                optExcelCell(row, idx, "external_reference"),
                optExcelCell(row, idx, "payment_reference"),
                optExcelCell(row, idx, "service_period"),
                optExcelCell(row, idx, "description"),
                optExcelCell(row, idx, "remarks"));
    }

    private record RowData(Retailer retailer, StbAsset asset, RechargeType type, LocalDate date,
                           BigDecimal amount, PaymentMethod paymentMethod, String reference,
                           String externalReference, String paymentReference,
                           String servicePeriod, String description, String remarks) {}

    // ── Validation helpers ────────────────────────────────────────────────────

    private Retailer findRetailer(Tenant tenant, String code, int rowNum) {
        return retailerRepository.findByTenantIdAndRetailerCode(tenant.getId(), code.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("INVALID_RETAILER",
                        "Row " + rowNum + ": Retailer not found: " + code));
    }

    private StbAsset findAsset(Tenant tenant, String serial, int rowNum) {
        if (serial == null || serial.isBlank()) return null;
        return assetRepository.findByTenantIdAndSerialNumber(tenant.getId(), serial.trim())
                .orElseThrow(() -> new BusinessException("INVALID_ASSET",
                        "Row " + rowNum + ": Asset not found: " + serial));
    }

    private LocalDate parseDate(String s, int rowNum) {
        try { return LocalDate.parse(s.trim()); }
        catch (DateTimeParseException e) {
            throw new BusinessException("INVALID_DATE", "Row " + rowNum + ": Invalid date: " + s);
        }
    }

    private BigDecimal parseAmount(String s, int rowNum) {
        try {
            BigDecimal v = new BigDecimal(s.trim());
            if (v.compareTo(BigDecimal.ZERO) <= 0)
                throw new BusinessException("INVALID_AMOUNT", "Row " + rowNum + ": Amount must be positive");
            return v;
        } catch (NumberFormatException e) {
            throw new BusinessException("INVALID_AMOUNT", "Row " + rowNum + ": Invalid amount: " + s);
        }
    }

    private RechargeType parseRechargeType(String s, int rowNum) {
        try { return RechargeType.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_TYPE", "Row " + rowNum + ": Invalid recharge type: " + s);
        }
    }

    private PaymentMethod parsePaymentMethod(String s) {
        if (s == null || s.isBlank()) return null;
        try { return PaymentMethod.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private String require(String val, String col, int rowNum) {
        if (val == null || val.isBlank())
            throw new BusinessException("MISSING_FIELD", "Row " + rowNum + ": " + col + " is required");
        return val.trim();
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) map.put(headers[i].trim().toLowerCase(), i);
        return map;
    }

    private Map<String, Integer> buildExcelHeaderIndex(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Cell cell : headerRow) map.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
        return map;
    }

    private void validateRequiredHeaders(Map<String, Integer> idx) {
        List<String> missing = REQUIRED_HEADERS.stream().filter(h -> !idx.containsKey(h)).toList();
        if (!missing.isEmpty())
            throw new BusinessException("MISSING_HEADERS", "Missing required columns: " + missing);
    }

    private String cell(String[] row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null || i >= row.length) return "";
        return row[i] != null ? row[i] : "";
    }

    private String optCell(String[] row, Map<String, Integer> idx, String col) {
        String v = cell(row, idx, col).trim();
        return v.isBlank() ? null : v;
    }

    private String excelCell(Row row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null) return "";
        Cell cell = row.getCell(i);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private String optExcelCell(Row row, Map<String, Integer> idx, String col) {
        String v = excelCell(row, idx, col).trim();
        return v.isBlank() ? null : v;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        return true;
    }

    private Tenant resolveTenant() {
        String code = TenantContext.getCurrentTenant();
        if (code == null || code.isBlank())
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        return tenantRepository.findByTenantCode(code)
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant not found: " + code));
    }
}
