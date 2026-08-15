package org.nexus.d2h.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ReportExportService {

    private static final String[] RETAILER_HEADERS =
            {"Retailer Code", "Retailer Name", "Box Sales", "Received", "Outstanding", "Recharge"};
    private static final String[] PERIOD_HEADERS =
            {"Date From", "Date To", "Box Sales", "Received", "Outstanding", "Recharge", "Transaction Count"};

    // ── CSV exports ───────────────────────────────────────────────────────────

    public byte[] retailerReportCsv(List<RetailerReportDto> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", RETAILER_HEADERS)).append("\n");
        for (RetailerReportDto r : rows) {
            sb.append(csv(r.retailerCode())).append(",")
              .append(csv(r.retailerName())).append(",")
              .append(r.boxSales()).append(",")
              .append(r.received()).append(",")
              .append(r.outstanding()).append(",")
              .append(r.recharge()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] periodReportCsv(PeriodReportDto r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", PERIOD_HEADERS)).append("\n");
        sb.append(r.dateFrom()).append(",")
          .append(r.dateTo()).append(",")
          .append(r.boxSales()).append(",")
          .append(r.received()).append(",")
          .append(r.outstanding()).append(",")
          .append(r.recharge()).append(",")
          .append(r.transactionCount()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Excel exports ─────────────────────────────────────────────────────────

    public byte[] retailerReportExcel(List<RetailerReportDto> rows) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Retailer Report");
            CellStyle headerStyle = boldStyle(wb);
            writeRow(sheet.createRow(0), RETAILER_HEADERS, headerStyle);
            int rowIdx = 1;
            for (RetailerReportDto r : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.retailerCode());
                row.createCell(1).setCellValue(r.retailerName());
                setAmount(row.createCell(2), r.boxSales());
                setAmount(row.createCell(3), r.received());
                setAmount(row.createCell(4), r.outstanding());
                setAmount(row.createCell(5), r.recharge());
            }
            autoSize(sheet, RETAILER_HEADERS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    public byte[] periodReportExcel(PeriodReportDto r) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Period Report");
            CellStyle headerStyle = boldStyle(wb);
            writeRow(sheet.createRow(0), PERIOD_HEADERS, headerStyle);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(r.dateFrom() != null ? r.dateFrom().toString() : "");
            row.createCell(1).setCellValue(r.dateTo() != null ? r.dateTo().toString() : "");
            setAmount(row.createCell(2), r.boxSales());
            setAmount(row.createCell(3), r.received());
            setAmount(row.createCell(4), r.outstanding());
            setAmount(row.createCell(5), r.recharge());
            row.createCell(6).setCellValue(r.transactionCount());
            autoSize(sheet, PERIOD_HEADERS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void writeRow(Row row, String[] values, CellStyle style) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            cell.setCellStyle(style);
        }
    }

    private void setAmount(Cell cell, BigDecimal value) {
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
    }

    private CellStyle boldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
