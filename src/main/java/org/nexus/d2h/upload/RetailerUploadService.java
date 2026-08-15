package org.nexus.d2h.upload;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.retailer.CreateRetailerRequest;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class RetailerUploadService {

    private static final int BATCH_SIZE = 100;
    private static final List<String> REQUIRED_HEADERS = List.of(
            "retailer_code", "retailer_name", "mobile"
    );
    private static final List<String> ALL_HEADERS = List.of(
            "retailer_code", "retailer_name", "mobile", "alternate_mobile",
            "email", "address", "city", "state", "pin_code", "gst_number",
            "pan_number", "joining_date"
    );

    private final RetailerRepository retailerRepository;
    private final TenantRepository tenantRepository;

    public RetailerUploadService(RetailerRepository retailerRepository, TenantRepository tenantRepository) {
        this.retailerRepository = retailerRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public UploadResult upload(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        if (file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Uploaded file is empty");
        }

        if (filename.endsWith(".csv")) {
            return processCsv(file);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return processExcel(file);
        } else {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE", "Only CSV and Excel files are supported");
        }
    }

    private UploadResult processCsv(MultipartFile file) {
        Tenant tenant = resolveTenant();
        List<UploadResult.RowError> errors = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        List<Retailer> batch = new ArrayList<>();
        int[] counters = {0, 0, 0}; // total, success, duplicate

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new BusinessException("EMPTY_FILE", "CSV file has no content");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateRequiredHeaders(headerIndex);

            String[] row;
            int rowNum = 1;
            while ((row = reader.readNext()) != null) {
                rowNum++;
                counters[0]++;
                String rowData = String.join(",", row);

                try {
                    String code = cell(row, headerIndex, "retailer_code").trim().toUpperCase();
                    if (seenCodes.contains(code)) {
                        counters[2]++;
                        errors.add(new UploadResult.RowError(rowNum, rowData, "Duplicate retailer code in file: " + code));
                        continue;
                    }
                    if (retailerRepository.existsByTenantIdAndRetailerCode(tenant.getId(), code)) {
                        counters[2]++;
                        errors.add(new UploadResult.RowError(rowNum, rowData, "Retailer code already exists: " + code));
                        continue;
                    }

                    Retailer retailer = buildRetailer(tenant, row, headerIndex, code);
                    seenCodes.add(code);
                    batch.add(retailer);

                    if (batch.size() >= BATCH_SIZE) {
                        retailerRepository.saveAll(batch);
                        counters[1] += batch.size();
                        batch.clear();
                    }
                } catch (BusinessException e) {
                    errors.add(new UploadResult.RowError(rowNum, rowData, e.getMessage()));
                }
            }

            if (!batch.isEmpty()) {
                retailerRepository.saveAll(batch);
                counters[1] += batch.size();
            }

        } catch (IOException | CsvValidationException e) {
            throw new BusinessException("FILE_READ_ERROR", "Failed to read CSV file: " + e.getMessage());
        }

        int failed = counters[0] - counters[1] - counters[2];
        log.info("Retailer CSV upload complete: total={} success={} failed={} duplicate={} tenant={}",
                counters[0], counters[1], failed, counters[2], tenant.getTenantCode());
        return new UploadResult(UUID.randomUUID().toString(), counters[0], counters[1], failed, counters[2], errors);
    }

    private UploadResult processExcel(MultipartFile file) {
        Tenant tenant = resolveTenant();
        List<UploadResult.RowError> errors = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        List<Retailer> batch = new ArrayList<>();
        int[] counters = {0, 0, 0};

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("EMPTY_FILE", "Excel file has no content");
            }

            Map<String, Integer> headerIndex = buildExcelHeaderIndex(headerRow);
            validateRequiredHeaders(headerIndex);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                counters[0]++;
                String rowData = "row " + (i + 1);

                try {
                    String code = excelCell(row, headerIndex, "retailer_code").trim().toUpperCase();
                    if (seenCodes.contains(code)) {
                        counters[2]++;
                        errors.add(new UploadResult.RowError(i + 1, rowData, "Duplicate retailer code in file: " + code));
                        continue;
                    }
                    if (retailerRepository.existsByTenantIdAndRetailerCode(tenant.getId(), code)) {
                        counters[2]++;
                        errors.add(new UploadResult.RowError(i + 1, rowData, "Retailer code already exists: " + code));
                        continue;
                    }

                    Retailer retailer = buildRetailerFromExcel(tenant, row, headerIndex, code);
                    seenCodes.add(code);
                    batch.add(retailer);

                    if (batch.size() >= BATCH_SIZE) {
                        retailerRepository.saveAll(batch);
                        counters[1] += batch.size();
                        batch.clear();
                    }
                } catch (BusinessException e) {
                    errors.add(new UploadResult.RowError(i + 1, rowData, e.getMessage()));
                }
            }

            if (!batch.isEmpty()) {
                retailerRepository.saveAll(batch);
                counters[1] += batch.size();
            }

        } catch (IOException e) {
            throw new BusinessException("FILE_READ_ERROR", "Failed to read Excel file: " + e.getMessage());
        }

        int failed = counters[0] - counters[1] - counters[2];
        log.info("Retailer Excel upload complete: total={} success={} failed={} duplicate={} tenant={}",
                counters[0], counters[1], failed, counters[2], tenant.getTenantCode());
        return new UploadResult(UUID.randomUUID().toString(), counters[0], counters[1], failed, counters[2], errors);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Retailer buildRetailer(Tenant tenant, String[] row, Map<String, Integer> idx, String code) {
        String name = cell(row, idx, "retailer_name").trim();
        String mobile = cell(row, idx, "mobile").trim();
        if (name.isBlank()) throw new BusinessException("VALIDATION_ERROR", "retailer_name is required");
        if (!mobile.matches("^[0-9]{10}$")) throw new BusinessException("VALIDATION_ERROR", "Invalid mobile: " + mobile);

        Retailer r = new Retailer();
        r.setTenant(tenant);
        r.setRetailerCode(code);
        r.setRetailerName(name);
        r.setMobile(mobile);
        r.setAlternateMobile(optCell(row, idx, "alternate_mobile"));
        r.setEmail(optCell(row, idx, "email"));
        r.setAddress(optCell(row, idx, "address"));
        r.setCity(optCell(row, idx, "city"));
        r.setState(optCell(row, idx, "state"));
        r.setPinCode(optCell(row, idx, "pin_code"));
        r.setGstNumber(optCell(row, idx, "gst_number"));
        r.setPanNumber(optCell(row, idx, "pan_number"));
        r.setStatus(RetailerStatus.ACTIVE);
        String joiningDateStr = optCell(row, idx, "joining_date");
        if (joiningDateStr != null && !joiningDateStr.isBlank()) {
            try { r.setJoiningDate(LocalDate.parse(joiningDateStr)); }
            catch (DateTimeParseException e) { /* ignore invalid date */ }
        }
        return r;
    }

    private Retailer buildRetailerFromExcel(Tenant tenant, Row row, Map<String, Integer> idx, String code) {
        String name = excelCell(row, idx, "retailer_name").trim();
        String mobile = excelCell(row, idx, "mobile").trim();
        if (name.isBlank()) throw new BusinessException("VALIDATION_ERROR", "retailer_name is required");
        if (!mobile.matches("^[0-9]{10}$")) throw new BusinessException("VALIDATION_ERROR", "Invalid mobile: " + mobile);

        Retailer r = new Retailer();
        r.setTenant(tenant);
        r.setRetailerCode(code);
        r.setRetailerName(name);
        r.setMobile(mobile);
        r.setAlternateMobile(optExcelCell(row, idx, "alternate_mobile"));
        r.setEmail(optExcelCell(row, idx, "email"));
        r.setAddress(optExcelCell(row, idx, "address"));
        r.setCity(optExcelCell(row, idx, "city"));
        r.setState(optExcelCell(row, idx, "state"));
        r.setPinCode(optExcelCell(row, idx, "pin_code"));
        r.setGstNumber(optExcelCell(row, idx, "gst_number"));
        r.setPanNumber(optExcelCell(row, idx, "pan_number"));
        r.setStatus(RetailerStatus.ACTIVE);
        return r;
    }

    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim().toLowerCase(), i);
        }
        return map;
    }

    private Map<String, Integer> buildExcelHeaderIndex(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            map.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
        }
        return map;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> missing = REQUIRED_HEADERS.stream()
                .filter(h -> !headerIndex.containsKey(h))
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("MISSING_HEADERS", "Missing required columns: " + missing);
        }
    }

    private String cell(String[] row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null || i >= row.length) return "";
        return row[i] != null ? row[i] : "";
    }

    private String optCell(String[] row, Map<String, Integer> idx, String col) {
        String val = cell(row, idx, col).trim();
        return val.isBlank() ? null : val;
    }

    private String excelCell(Row row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null) return "";
        Cell cell = row.getCell(i);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private String optExcelCell(Row row, Map<String, Integer> idx, String col) {
        String val = excelCell(row, idx, col).trim();
        return val.isBlank() ? null : val;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private Tenant resolveTenant() {
        String tenantCode = TenantContext.getCurrentTenant();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
        return tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant not found: " + tenantCode));
    }
}
