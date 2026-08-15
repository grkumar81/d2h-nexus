package org.nexus.d2h.finance;

import jakarta.validation.Valid;
import org.nexus.d2h.common.ApiResponse;
import org.nexus.d2h.common.PageResponse;
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
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> create(
            @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(financeService.create(request)));
    }

    @GetMapping("/transactions/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getById(id)));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PageResponse<FinancialTransactionDto>>> list(
            @RequestParam(required = false) Long retailerId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(financeService.search(
                retailerId, type, status, paymentMethod, dateFrom, dateTo,
                reference, amountMin, amountMax, pageable)));
    }

    @PostMapping("/transactions/{id}/reverse")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> reverse(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.reverse(id, reason)));
    }

    @PostMapping("/transactions/{id}/adjust")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> adjust(
            @PathVariable Long id,
            @Valid @RequestBody AdjustTransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.adjust(id, request)));
    }

    @GetMapping("/retailers/{retailerId}/summary")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<RetailerFinanceSummaryDto>> retailerSummary(
            @PathVariable Long retailerId) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getRetailerSummary(retailerId)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_USER','OPERATIONS_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<FinanceSummaryDto>> tenantSummary() {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getTenantSummary()));
    }
}
