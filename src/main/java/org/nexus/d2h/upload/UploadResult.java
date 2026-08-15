package org.nexus.d2h.upload;

import java.util.List;

public record UploadResult(
        String uploadId,
        int totalRecords,
        int successRecords,
        int failedRecords,
        int duplicateRecords,
        List<RowError> errors
) {
    public record RowError(int rowNumber, String rowData, String errorMessage) {}
}
