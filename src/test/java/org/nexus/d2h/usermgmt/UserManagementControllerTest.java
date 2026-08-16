package org.nexus.d2h.usermgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nexus.d2h.auth.JwtProperties;
import org.nexus.d2h.auth.JwtService;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserManagementController.class)
class UserManagementControllerTest extends org.nexus.d2h.BaseControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean UserManagementService userManagementService;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtProperties jwtProperties;

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void createUser_validRequest_returns201() throws Exception {
        UserDto dto = sampleUser();
        when(userManagementService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("newuser", "new@example.com",
                                        "password123", "New User", null, Set.of("READ_ONLY")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void createUser_duplicateUsername_returns422() throws Exception {
        when(userManagementService.create(any()))
                .thenThrow(new BusinessException("DUPLICATE_USERNAME", "Username already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("alice", "alice@example.com",
                                        "password123", null, null, Set.of("READ_ONLY")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void listUsers_returns200() throws Exception {
        when(userManagementService.listForTenant(any()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(sampleUser()))));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void activateUser_returns200() throws Exception {
        when(userManagementService.activate(eq(5L))).thenReturn(sampleUser());

        mockMvc.perform(post("/api/v1/users/5/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void deactivateUser_returns200() throws Exception {
        when(userManagementService.deactivate(eq(5L))).thenReturn(sampleUser());

        mockMvc.perform(post("/api/v1/users/5/deactivate"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice")
    void changePassword_validRequest_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldpass1", "newpass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "alice")
    void changePassword_blankNewPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldpass1", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private UserDto sampleUser() {
        return new UserDto(5L, "alice", "alice@example.com", "Alice",
                null, "ACTIVE", Set.of("TENANT_ADMIN"), Instant.now());
    }
}
