package org.nexus.d2h.retailer;

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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests — security chain not loaded.
 * Authorization and unauthenticated access are tested in SecurityConfigTest.
 */
@WebMvcTest(RetailerController.class)
class RetailerControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RetailerService retailerService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_validRequest_returns201() throws Exception {
        when(retailerService.create(any())).thenReturn(sampleDto());

        mockMvc.perform(post("/api/v1/retailers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.retailerCode").value("RET001"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_duplicateCode_returns422() throws Exception {
        when(retailerService.create(any()))
                .thenThrow(new BusinessException("DUPLICATE_RETAILER_CODE", "Retailer code 'RET001' already exists"));

        mockMvc.perform(post("/api/v1/retailers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RETAILER_CODE"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void create_missingRequiredFields_returns400() throws Exception {
        String body = "{\"retailerCode\":\"\",\"retailerName\":\"\",\"mobile\":\"\"}";

        mockMvc.perform(post("/api/v1/retailers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_existingRetailer_returns200() throws Exception {
        when(retailerService.getById(1L)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/retailers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retailerCode").value("RET001"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void getById_notFound_returns404() throws Exception {
        when(retailerService.getById(99L)).thenThrow(new ResourceNotFoundException("Retailer", 99L));

        mockMvc.perform(get("/api/v1/retailers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void update_validRequest_returns200() throws Exception {
        when(retailerService.update(eq(1L), any())).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/retailers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retailerName").value("Test Retailer"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void list_returnsPagedResults() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleDto())));
        when(retailerService.search(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/retailers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].retailerCode").value("RET001"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void list_withStatusFilter_passesStatusToService() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleDto())));
        when(retailerService.search(isNull(), eq(RetailerStatus.ACTIVE), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/retailers?status=ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void activate_returns200WithActiveStatus() throws Exception {
        when(retailerService.activate(1L)).thenReturn(dtoWithStatus(RetailerStatus.ACTIVE));

        mockMvc.perform(patch("/api/v1/retailers/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void deactivate_returns200WithInactiveStatus() throws Exception {
        when(retailerService.deactivate(1L)).thenReturn(dtoWithStatus(RetailerStatus.INACTIVE));

        mockMvc.perform(patch("/api/v1/retailers/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RetailerDto sampleDto() {
        return new RetailerDto(1L, "RET001", "Test Retailer", "9876543210",
                null, null, null, "Mumbai", "Maharashtra", "400001", null, null,
                RetailerStatus.ACTIVE, null, "admin", null, Instant.now(), Instant.now());
    }

    private RetailerDto dtoWithStatus(RetailerStatus status) {
        return new RetailerDto(1L, "RET001", "Test Retailer", "9876543210",
                null, null, null, "Mumbai", "Maharashtra", "400001", null, null,
                status, null, "admin", null, Instant.now(), Instant.now());
    }

    private CreateRetailerRequest createRequest() {
        return new CreateRetailerRequest("RET001", "Test Retailer", "9876543210",
                null, null, null, "Mumbai", "Maharashtra", "400001", null, null, null);
    }

    private UpdateRetailerRequest updateRequest() {
        return new UpdateRetailerRequest("Test Retailer", "9876543210",
                null, null, null, "Mumbai", "Maharashtra", "400001", null, null, null);
    }
}
