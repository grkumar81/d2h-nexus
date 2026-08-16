package org.nexus.d2h.asset;

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

@WebMvcTest(AssetController.class)
class AssetControllerTest extends org.nexus.d2h.BaseControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AssetService assetService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_validRequest_returns201() throws Exception {
        when(assetService.create(any())).thenReturn(sampleDto());

        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAssetRequest("SN001", "BX001", "ModelX", "MfgA", "B1", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serialNumber").value("SN001"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_duplicateSerial_returns422() throws Exception {
        when(assetService.create(any()))
                .thenThrow(new BusinessException("DUPLICATE_SERIAL_NUMBER", "Serial number 'SN001' already exists"));

        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAssetRequest("SN001", null, null, null, null, null, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SERIAL_NUMBER"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_missingSerial_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serialNumber\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_found_returns200() throws Exception {
        when(assetService.getById(1L)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/assets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serialNumber").value("SN001"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_notFound_returns404() throws Exception {
        when(assetService.getById(99L)).thenThrow(new ResourceNotFoundException("Asset", 99L));

        mockMvc.perform(get("/api/v1/assets/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OPERATIONS_USER")
    void tag_validRequest_returns200() throws Exception {
        when(assetService.tag(eq(1L), any())).thenReturn(sampleDto(AssetStatus.ALLOCATED));

        mockMvc.perform(patch("/api/v1/assets/1/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retailerId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ALLOCATED"));
    }

    @Test
    @WithMockUser(roles = "OPERATIONS_USER")
    void tag_untaggableAsset_returns422() throws Exception {
        when(assetService.tag(eq(1L), any()))
                .thenThrow(new BusinessException("ASSET_NOT_TAGGABLE", "Asset in status SOLD cannot be tagged"));

        mockMvc.perform(patch("/api/v1/assets/1/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retailerId\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ASSET_NOT_TAGGABLE"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void list_returnsPagedResults() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleDto())));
        when(assetService.search(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].serialNumber").value("SN001"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void history_returns200() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleHistoryDto())));
        when(assetService.getHistory(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/assets/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].toStatus").value("AVAILABLE"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AssetDto sampleDto() {
        return sampleDto(AssetStatus.AVAILABLE);
    }

    private AssetDto sampleDto(AssetStatus status) {
        return new AssetDto(1L, "SN001", "BX001", "ModelX", "MfgA", "B1",
                LocalDate.now(), BigDecimal.valueOf(1500), status,
                null, null, null, null, null, null, null,
                "admin", null, Instant.now(), Instant.now());
    }

    private AssetHistoryDto sampleHistoryDto() {
        return new AssetHistoryDto(1L, 1L, null, AssetStatus.AVAILABLE, null, null, "admin", "Asset created", Instant.now());
    }
}
