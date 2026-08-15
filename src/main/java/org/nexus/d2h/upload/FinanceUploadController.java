package org.nexus.d2h.upload;

import org.nexus.d2h.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceUploadController {

    private final FinanceUploadService financeUploadService;

    public FinanceUploadController(FinanceUploadService financeUploadService) {
        this.financeUploadService = financeUploadService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<FinanceUploadResult>> upload(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(financeUploadService.upload(file)));
    }
}
