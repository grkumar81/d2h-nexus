package org.nexus.d2h.boxsale;

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

@WebMvcTest(BoxSaleController.class)
class BoxSaleControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BoxSaleService boxSaleService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_validRequest_returns201() throws Exception {
        when(boxSaleService.create(any())).thenReturn(sampleSaleDto());

        var request = new CreateSaleRequest(5L, LocalDate.now(),
                List.of(new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500))),
                "REF001", null);

        mockMvc.perform(post("/api/v1/box-sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_unavailableAsset_returns422() throws Exception {
        when(boxSaleService.create(any()))
                .thenThrow(new BusinessException("ASSET_NOT_SALEABLE", "Asset is not available for sale"));

        var request = new CreateSaleRequest(5L, LocalDate.now(),
                List.of(new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500))),
                null, null);

        mockMvc.perform(post("/api/v1/box-sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ASSET_NOT_SALEABLE"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_missingItems_returns400() throws Exception {
        String body = "{\"retailerId\":5,\"transactionDate\":\"2025-01-01\",\"items\":[]}";

        mockMvc.perform(post("/api/v1/box-sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_found_returns200() throws Exception {
        when(boxSaleService.getById(1L)).thenReturn(sampleSaleDto());

        mockMvc.perform(get("/api/v1/box-sales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retailerCode").value("RET001"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_notFound_returns404() throws Exception {
        when(boxSaleService.getById(99L)).thenThrow(new ResourceNotFoundException("Sale", 99L));

        mockMvc.perform(get("/api/v1/box-sales/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void list_returnsPagedResults() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleSaleDto())));
        when(boxSaleService.list(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/box-sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SaleDto sampleSaleDto() {
        var item = new SaleItemDto(1L, 10L, "SN0010", "BX001", BigDecimal.valueOf(1500));
        return new SaleDto(1L, 5L, "RET001", "Test Retailer",
                LocalDate.now(), BigDecimal.valueOf(1500), PaymentStatus.PENDING,
                "REF001", null, List.of(item), "admin", Instant.now());
    }
}
