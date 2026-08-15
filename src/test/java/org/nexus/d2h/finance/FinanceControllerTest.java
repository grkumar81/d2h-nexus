package org.nexus.d2h.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.nexus.d2h.auth.JwtProperties;
import org.nexus.d2h.auth.JwtService;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FinanceController.class)
class FinanceControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean FinanceService financeService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void create_validRequest_returns201() throws Exception {
        when(financeService.create(any())).thenReturn(sampleDto());

        var request = new CreateTransactionRequest(5L, TransactionType.PAYMENT_RECEIVED,
                LocalDate.now(), BigDecimal.valueOf(5000), PaymentMethod.CASH,
                "REF001", null, null, null);

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionType").value("PAYMENT_RECEIVED"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void create_duplicateReference_returns422() throws Exception {
        when(financeService.create(any()))
                .thenThrow(new BusinessException("DUPLICATE_REFERENCE", "Reference already exists"));

        var request = new CreateTransactionRequest(5L, TransactionType.PAYMENT_RECEIVED,
                LocalDate.now(), BigDecimal.valueOf(5000), null, "REF001", null, null, null);

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REFERENCE"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void create_missingAmount_returns400() throws Exception {
        String body = "{\"retailerId\":5,\"transactionType\":\"PAYMENT_RECEIVED\",\"transactionDate\":\"2025-01-01\"}";

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_found_returns200() throws Exception {
        when(financeService.getById(1L)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/finance/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("PAYMENT_RECEIVED"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_notFound_returns404() throws Exception {
        when(financeService.getById(99L)).thenThrow(new ResourceNotFoundException("FinancialTransaction", 99L));

        mockMvc.perform(get("/api/v1/finance/transactions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void list_returnsPagedResults() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleDto())));
        when(financeService.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/finance/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void reverse_returns200() throws Exception {
        when(financeService.reverse(eq(1L), any())).thenReturn(reversalDto());

        mockMvc.perform(post("/api/v1/finance/transactions/1/reverse")
                        .param("reason", "Incorrect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("REVERSAL"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void reverse_alreadyReversed_returns422() throws Exception {
        when(financeService.reverse(eq(1L), any()))
                .thenThrow(new BusinessException("ALREADY_REVERSED", "Already reversed"));

        mockMvc.perform(post("/api/v1/finance/transactions/1/reverse"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ALREADY_REVERSED"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void retailerSummary_returns200() throws Exception {
        when(financeService.getRetailerSummary(5L)).thenReturn(
                new RetailerFinanceSummaryDto(5L, "RET001", "Test Retailer",
                        BigDecimal.valueOf(100000), BigDecimal.valueOf(100000),
                        BigDecimal.valueOf(70000), BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(15000)));

        mockMvc.perform(get("/api/v1/finance/retailers/5/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outstanding").value(30000));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void tenantSummary_returns200() throws Exception {
        when(financeService.getTenantSummary()).thenReturn(
                new FinanceSummaryDto(BigDecimal.valueOf(200000), BigDecimal.valueOf(150000),
                        BigDecimal.valueOf(50000), BigDecimal.valueOf(30000), 50L));

        mockMvc.perform(get("/api/v1/finance/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outstanding").value(50000));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private FinancialTransactionDto sampleDto() {
        return new FinancialTransactionDto(1L, 5L, "RET001", "Test Retailer",
                TransactionType.PAYMENT_RECEIVED, TransactionStatus.POSTED,
                LocalDate.now(), BigDecimal.valueOf(5000), PaymentMethod.CASH,
                "REF001", null, null, null, TransactionSource.MANUAL,
                null, null, null, "user", null, Instant.now(), Instant.now());
    }

    private FinancialTransactionDto reversalDto() {
        return new FinancialTransactionDto(2L, 5L, "RET001", "Test Retailer",
                TransactionType.REVERSAL, TransactionStatus.POSTED,
                LocalDate.now(), BigDecimal.valueOf(-5000), null,
                "REV-1-abc", null, "Reversal of #1", null, TransactionSource.MANUAL,
                null, null, 1L, "user", null, Instant.now(), Instant.now());
    }
}
