package org.nexus.d2h.upload;

import java.math.BigDecimal;
import java.util.List;

public record RechargeUploadResult(
        String uploadId,
        int totalRecords,
        int successRecords,
        int failedRecords,
        int duplicateRecords,
        BigDecimal totalAmountProcessed,
        List<UploadResult.RowError> errors
) {}
