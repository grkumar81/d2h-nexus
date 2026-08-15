package org.nexus.d2h.upload;

import org.nexus.d2h.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/retailers/upload")
public class RetailerUploadController {

    private final RetailerUploadService uploadService;

    public RetailerUploadController(RetailerUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<UploadResult>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(uploadService.upload(file)));
    }
}
