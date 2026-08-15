package org.nexus.d2h.upload;

import org.nexus.d2h.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/recharges/upload")
public class RechargeUploadController {

    private final RechargeUploadService rechargeUploadService;

    public RechargeUploadController(RechargeUploadService rechargeUploadService) {
        this.rechargeUploadService = rechargeUploadService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<RechargeUploadResult>> upload(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(rechargeUploadService.upload(file)));
    }
}
