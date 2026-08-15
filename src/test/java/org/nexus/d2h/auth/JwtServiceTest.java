package org.nexus.d2h.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // 256-bit Base64-encoded secret for tests
        props.setSecret("dGVzdC1zZWNyZXQtZm9yLXVuaXQtdGVzdHMtbXVzdC1iZS0yNTYtYml0cy1sb25n");
        props.setExpirationMs(3600000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAndValidate_returnsCorrectPrincipal() {
        String token = jwtService.generateToken(1L, "alice", "DIST001", Set.of("TENANT_ADMIN"));

        D2HPrincipal principal = jwtService.validateAndExtract(token);

        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.tenantCode()).isEqualTo("DIST001");
        assertThat(principal.roles()).containsExactly("TENANT_ADMIN");
    }

    @Test
    void generateAndValidate_multipleRoles() {
        String token = jwtService.generateToken(2L, "bob", "DIST002", Set.of("FINANCE_USER", "READ_ONLY"));

        D2HPrincipal principal = jwtService.validateAndExtract(token);

        assertThat(principal.roles()).containsExactlyInAnyOrder("FINANCE_USER", "READ_ONLY");
    }

    @Test
    void generateAndValidate_nullTenantCode_isAllowed() {
        String token = jwtService.generateToken(3L, "platform_admin", null, Set.of("TENANT_ADMIN"));

        D2HPrincipal principal = jwtService.validateAndExtract(token);

        assertThat(principal.tenantCode()).isNull();
    }

    @Test
    void validate_tamperedToken_throwsJwtException() {
        String token = jwtService.generateToken(1L, "alice", "DIST001", Set.of("TENANT_ADMIN"));
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.validateAndExtract(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validate_expiredToken_throwsJwtException() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("dGVzdC1zZWNyZXQtZm9yLXVuaXQtdGVzdHMtbXVzdC1iZS0yNTYtYml0cy1sb25n");
        shortProps.setExpirationMs(-1000L); // already expired
        JwtService shortLivedService = new JwtService(shortProps);

        String token = shortLivedService.generateToken(1L, "alice", "DIST001", Set.of());

        assertThatThrownBy(() -> jwtService.validateAndExtract(token))
                .isInstanceOf(JwtException.class);
    }
}
