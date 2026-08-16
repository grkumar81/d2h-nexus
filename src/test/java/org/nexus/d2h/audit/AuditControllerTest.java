package org.nexus.d2h.audit;

import org.junit.jupiter.api.Test;
import org.nexus.d2h.auth.JwtProperties;
import org.nexus.d2h.auth.JwtService;
import org.nexus.d2h.common.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
class AuditControllerTest extends org.nexus.d2h.BaseControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuditService auditService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void search_returns200WithResults() throws Exception {
        AuditLogDto dto = new AuditLogDto(1L, "Retailer", "42", "CREATE",
                "admin", "code=RET001", null, Instant.now());
        when(auditService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(PageResponse.from(
                        new org.springframework.data.domain.PageImpl<>(List.of(dto))));

        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].entityType").value("Retailer"))
                .andExpect(jsonPath("$.data.content[0].action").value("CREATE"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void search_withFilters_returns200() throws Exception {
        when(auditService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(PageResponse.from(
                        new org.springframework.data.domain.PageImpl<>(List.of())));

        mockMvc.perform(get("/api/v1/audit")
                        .param("entityType", "FinancialTransaction")
                        .param("action", "CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
