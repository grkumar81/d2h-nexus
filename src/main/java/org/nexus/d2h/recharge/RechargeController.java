package org.nexus.d2h.recharge;

import jakarta.validation.Valid;
import org.nexus.d2h.common.ApiResponse;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.finance.PaymentMethod;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/recharges")
public class RechargeController {

    private final RechargeService rechargeService;

    public RechargeController(RechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<RechargeTransactionDto>> create(
            @Valid @RequestBody CreateRechargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(rechargeService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<RechargeTransactionDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(rechargeService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PageResponse<RechargeTransactionDto>>> list(
            @RequestParam(required = false) Long retailerId,
            @RequestParam(required = false) RechargeType type,
            @RequestParam(required = false) RechargeStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "rechargeDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(rechargeService.search(
                retailerId, type, status, paymentMethod, dateFrom, dateTo,
                reference, amountMin, amountMax, pageable)));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<RechargeTransactionDto>> reverse(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok(rechargeService.reverse(id, reason)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<RechargeTransactionDto>> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok(rechargeService.cancel(id, reason)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<RechargeSummaryDto>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.ok(rechargeService.getSummary(dateFrom, dateTo)));
    }

    @GetMapping("/retailers/{retailerId}/summary")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<RetailerRechargeSummaryDto>> retailerSummary(
            @PathVariable Long retailerId) {
        return ResponseEntity.ok(ApiResponse.ok(rechargeService.getRetailerSummary(retailerId)));
    }
}
