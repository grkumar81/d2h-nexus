package org.nexus.d2h.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class NotificationTemplateService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String buildSubject(NotificationEventType eventType, Map<String, Object> data) {
        String retailer = str(data, "retailerName", str(data, "retailerCode", "Retailer"));
        return switch (eventType) {
            case FINANCE_TRANSACTION_CREATED  -> "Payment Received — " + retailer;
            case FINANCE_TRANSACTION_REVERSED -> "Transaction Reversed — " + retailer;
            case FINANCE_TRANSACTION_ADJUSTED -> "Transaction Adjusted — " + retailer;
            case FINANCE_UPLOAD_COMPLETED     -> "Finance Upload Completed";
            case RECHARGE_CREATED             -> "Recharge Recorded — " + retailer;
            case RECHARGE_REVERSED            -> "Recharge Reversed — " + retailer;
            case RECHARGE_UPLOAD_COMPLETED    -> "Recharge Upload Completed";
        };
    }

    public String buildEmailBody(NotificationEventType eventType, Map<String, Object> data) {
        return switch (eventType) {
            case FINANCE_TRANSACTION_CREATED,
                 FINANCE_TRANSACTION_REVERSED,
                 FINANCE_TRANSACTION_ADJUSTED -> buildFinanceBody(data);
            case FINANCE_UPLOAD_COMPLETED     -> buildUploadBody(data, "Finance");
            case RECHARGE_CREATED,
                 RECHARGE_REVERSED            -> buildRechargeBody(data);
            case RECHARGE_UPLOAD_COMPLETED    -> buildUploadBody(data, "Recharge");
        };
    }

    public String buildWhatsAppMessage(NotificationEventType eventType, Map<String, Object> data) {
        return switch (eventType) {
            case FINANCE_TRANSACTION_CREATED,
                 FINANCE_TRANSACTION_REVERSED,
                 FINANCE_TRANSACTION_ADJUSTED -> buildFinanceWhatsApp(data);
            case FINANCE_UPLOAD_COMPLETED     -> buildUploadWhatsApp(data, "Finance");
            case RECHARGE_CREATED,
                 RECHARGE_REVERSED            -> buildRechargeWhatsApp(data);
            case RECHARGE_UPLOAD_COMPLETED    -> buildUploadWhatsApp(data, "Recharge");
        };
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parsePayload(String payload) {
        try {
            return MAPPER.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse notification payload: {}", e.getMessage());
            return Map.of();
        }
    }

    // ── Private builders ──────────────────────────────────────────────────────

    private String buildFinanceBody(Map<String, Object> d) {
        return """
                D2H Distributor Management — Transaction Notification
                ======================================================
                Retailer:       %s
                Transaction:    %s
                Type:           %s
                Amount:         ₹%s
                Reference:      %s
                Date:           %s

                Financial Position
                ------------------
                Total Due:      ₹%s
                Total Received: ₹%s
                Outstanding:    ₹%s
                Total Recharge: ₹%s
                """.formatted(
                str(d, "retailerName", "-"),
                str(d, "transactionId", "-"),
                str(d, "transactionType", "-"),
                str(d, "amount", "0"),
                str(d, "reference", "-"),
                str(d, "transactionDate", "-"),
                str(d, "totalDue", "0"),
                str(d, "totalReceived", "0"),
                str(d, "outstanding", "0"),
                str(d, "totalRecharge", "0")
        );
    }

    private String buildFinanceWhatsApp(Map<String, Object> d) {
        return """
                *D2H Notification*
                Retailer: %s
                %s: ₹%s
                Ref: %s
                Total Due: ₹%s | Received: ₹%s | Outstanding: ₹%s
                """.formatted(
                str(d, "retailerName", "-"),
                str(d, "transactionType", "Transaction"),
                str(d, "amount", "0"),
                str(d, "reference", "-"),
                str(d, "totalDue", "0"),
                str(d, "totalReceived", "0"),
                str(d, "outstanding", "0")
        );
    }

    private String buildRechargeBody(Map<String, Object> d) {
        return """
                D2H Distributor Management — Recharge Notification
                ====================================================
                Retailer:       %s
                Recharge ID:    %s
                Type:           %s
                Amount:         ₹%s
                Reference:      %s
                Date:           %s
                Status:         %s
                """.formatted(
                str(d, "retailerName", "-"),
                str(d, "rechargeId", "-"),
                str(d, "rechargeType", "-"),
                str(d, "amount", "0"),
                str(d, "reference", "-"),
                str(d, "rechargeDate", "-"),
                str(d, "rechargeStatus", "-")
        );
    }

    private String buildRechargeWhatsApp(Map<String, Object> d) {
        return """
                *D2H Recharge*
                Retailer: %s | ₹%s
                Ref: %s | Status: %s
                """.formatted(
                str(d, "retailerName", "-"),
                str(d, "amount", "0"),
                str(d, "reference", "-"),
                str(d, "rechargeStatus", "-")
        );
    }

    private String buildUploadBody(Map<String, Object> d, String module) {
        return """
                D2H Distributor Management — %s Upload Completed
                ==================================================
                Total Records:  %s
                Successful:     %s
                Failed:         %s
                Duplicates:     %s
                Amount:         ₹%s
                """.formatted(
                module,
                str(d, "totalRows", "0"),
                str(d, "successCount", "0"),
                str(d, "failureCount", "0"),
                str(d, "duplicateCount", "0"),
                str(d, "totalAmount", "0")
        );
    }

    private String buildUploadWhatsApp(Map<String, Object> d, String module) {
        return "*D2H %s Upload* — %s/%s rows OK, ₹%s processed".formatted(
                module,
                str(d, "successCount", "0"),
                str(d, "totalRows", "0"),
                str(d, "totalAmount", "0")
        );
    }

    private String str(Map<String, Object> data, String key, String fallback) {
        Object v = data.get(key);
        return v != null ? v.toString() : fallback;
    }
}
