package org.nexus.d2h.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest extends org.nexus.d2h.BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @Test
    void unauthenticatedRequest_toProtectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/retailers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpoint_isPublic_withoutToken() throws Exception {
        // POST /api/v1/auth/login is public — should not return 401 even without a token
        // (it will return 400 because body is missing, not 401)
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Login endpoint should be public, got 401";
                });
    }
}
