package org.nexus.d2h.recharge;

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

@WebMvcTest(RechargeController.class)
class RechargeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RechargeService rechargeService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void create_validRequest_returns201() throws Exception {
        when(rechargeService.create(any())).thenReturn(sampleDto());

        var request = new CreateRechargeRequest(5L, null, LocalDate.now(),
                BigDecimal.valueOf(1000), RechargeType.REGULAR,
                null, null, null, null, null, null, "RCH001");

        mockMvc.perform(post("/api/v1/recharges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rechargeType").value("REGULAR"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void create_duplicateReference_returns422() throws Exception {
        when(rechargeService.create(any()))
                .thenThrow(new BusinessException("DUPLICATE_REFERENCE", "Reference already exists"));

        var request = new CreateRechargeRequest(5L, null, LocalDate.now(),
                BigDecimal.valueOf(1000), RechargeType.REGULAR,
                null, null, null, null, null, null, "DUP001");

        mockMvc.perform(post("/api/v1/recharges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REFERENCE"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void create_missingRetailerId_returns400() throws Exception {
        String body = "{\"rechargeDate\":\"2026-01-01\",\"amount\":1000,\"rechargeType\":\"REGULAR\"}";

        mockMvc.perform(post("/api/v1/recharges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_found_returns200() throws Exception {
        when(rechargeService.getById(1L)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/recharges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rechargeStatus").value("SUCCESS"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_notFound_returns404() throws Exception {
        when(rechargeService.getById(99L))
                .thenThrow(new ResourceNotFoundException("RechargeTransaction", 99L));

        mockMvc.perform(get("/api/v1/recharges/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void list_returnsPagedResults() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleDto())));
        when(rechargeService.search(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/recharges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void reverse_returns200() throws Exception {
        when(rechargeService.reverse(eq(1L), any())).thenReturn(reversalDto());

        mockMvc.perform(post("/api/v1/recharges/1/reverse")
                        .param("reason", "Wrong retailer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rechargeStatus").value("REVERSED"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void reverse_alreadyReversed_returns422() throws Exception {
        when(rechargeService.reverse(eq(1L), any()))
                .thenThrow(new BusinessException("ALREADY_REVERSED", "Already reversed"));

        mockMvc.perform(post("/api/v1/recharges/1/reverse"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ALREADY_REVERSED"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_USER")
    void cancel_returns200() throws Exception {
        RechargeTransactionDto cancelled = new RechargeTransactionDto(
                1L, 5L, "RET001", "Test Retailer", null, null,
                "RCH001", null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, RechargeStatus.CANCELLED, null, null,
                null, null, null, RechargeSource.MANUAL, null, null,
                "user", null, Instant.now(), Instant.now());
        when(rechargeService.cancel(eq(1L), any())).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/recharges/1/cancel")
                        .param("reason", "User request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rechargeStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void summary_returns200() throws Exception {
        when(rechargeService.getSummary(any(), any())).thenReturn(
                new RechargeSummaryDto(10L, BigDecimal.valueOf(50000),
                        BigDecimal.valueOf(48000), BigDecimal.valueOf(1000),
                        BigDecimal.valueOf(1000), null, null));

        mockMvc.perform(get("/api/v1/recharges/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(10));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void retailerSummary_returns200() throws Exception {
        when(rechargeService.getRetailerSummary(5L)).thenReturn(
                new RetailerRechargeSummaryDto(5L, "RET001", "Test Retailer",
                        10L, BigDecimal.valueOf(50000), BigDecimal.valueOf(48000),
                        LocalDate.now(), BigDecimal.valueOf(5000)));

        mockMvc.perform(get("/api/v1/recharges/retailers/5/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecharge").value(50000));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RechargeTransactionDto sampleDto() {
        return new RechargeTransactionDto(
                1L, 5L, "RET001", "Test Retailer", null, null,
                "RCH001", null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, RechargeStatus.SUCCESS, null, null,
                null, null, null, RechargeSource.MANUAL, null, null,
                "user", null, Instant.now(), Instant.now());
    }

    private RechargeTransactionDto reversalDto() {
        return new RechargeTransactionDto(
                2L, 5L, "RET001", "Test Retailer", null, null,
                "REV-1-ABC123", null, LocalDate.now(), BigDecimal.valueOf(-1000),
                RechargeType.REGULAR, RechargeStatus.REVERSED, null, null,
                null, "Reversal of recharge #1", null, RechargeSource.MANUAL, null, 1L,
                "user", null, Instant.now(), Instant.now());
    }
}
