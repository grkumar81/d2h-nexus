package org.nexus.d2h.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nexus.d2h.auth.JwtProperties;
import org.nexus.d2h.auth.JwtService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest extends org.nexus.d2h.BaseControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean NotificationService notificationService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── GET /config ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void listConfigs_returns200() throws Exception {
        when(notificationService.listConfigs()).thenReturn(List.of(sampleConfigDto()));

        mockMvc.perform(get("/api/v1/notifications/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].eventType").value("FINANCE_TRANSACTION_CREATED"))
                .andExpect(jsonPath("$.data[0].channel").value("EMAIL"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void listConfigs_empty_returns200WithEmptyList() throws Exception {
        when(notificationService.listConfigs()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── POST /config ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void saveConfig_validRequest_returns200() throws Exception {
        when(notificationService.saveConfig(any())).thenReturn(sampleConfigDto());

        String body = """
                {"eventType":"FINANCE_TRANSACTION_CREATED","channel":"EMAIL","enabled":true,"recipients":"test@example.com"}
                """;

        mockMvc.perform(post("/api/v1/notifications/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void saveConfig_missingEventType_returns400() throws Exception {
        String body = """
                {"channel":"EMAIL","enabled":true}
                """;

        mockMvc.perform(post("/api/v1/notifications/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void saveConfig_missingChannel_returns400() throws Exception {
        String body = """
                {"eventType":"FINANCE_TRANSACTION_CREATED","enabled":true}
                """;

        mockMvc.perform(post("/api/v1/notifications/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /config/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void deleteConfig_existing_returns200() throws Exception {
        doNothing().when(notificationService).deleteConfig(10L);

        mockMvc.perform(delete("/api/v1/notifications/config/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Configuration deleted"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void deleteConfig_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("NotificationConfig", 99L))
                .when(notificationService).deleteConfig(99L);

        mockMvc.perform(delete("/api/v1/notifications/config/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /history ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void history_returns200WithPage() throws Exception {
        var page = PageResponse.from(new PageImpl<>(List.of(sampleDeliveryDto())));
        when(notificationService.listDeliveries(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].channel").value("EMAIL"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void history_customPageSize_capped() throws Exception {
        var page = PageResponse.from(new PageImpl<NotificationDeliveryDto>(List.of()));
        when(notificationService.listDeliveries(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/history").param("size", "500"))
                .andExpect(status().isOk());

        verify(notificationService).listDeliveries(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private NotificationConfigDto sampleConfigDto() {
        return new NotificationConfigDto(10L, NotificationEventType.FINANCE_TRANSACTION_CREATED,
                NotificationChannel.EMAIL, true, "test@example.com", Instant.now());
    }

    private NotificationDeliveryDto sampleDeliveryDto() {
        return new NotificationDeliveryDto(1L, 100L,
                NotificationEventType.FINANCE_TRANSACTION_CREATED.name(),
                NotificationChannel.EMAIL, "test@example.com",
                NotificationStatus.SENT, 1, Instant.now(), null, Instant.now());
    }
}
