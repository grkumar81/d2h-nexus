package org.nexus.d2h.boxsale;

import jakarta.validation.Valid;
import org.nexus.d2h.common.ApiResponse;
import org.nexus.d2h.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/box-sales")
public class BoxSaleController {

    private final BoxSaleService boxSaleService;

    public BoxSaleController(BoxSaleService boxSaleService) {
        this.boxSaleService = boxSaleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER')")
    public ResponseEntity<ApiResponse<SaleDto>> create(@Valid @RequestBody CreateSaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(boxSaleService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER','FINANCE_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<SaleDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(boxSaleService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATIONS_USER','FINANCE_USER','READ_ONLY')")
    public ResponseEntity<ApiResponse<PageResponse<SaleDto>>> list(
            @RequestParam(required = false) Long retailerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(boxSaleService.list(retailerId, pageable)));
    }
}
