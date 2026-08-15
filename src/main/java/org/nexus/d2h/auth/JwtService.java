package org.nexus.d2h.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TENANT = "tenantCode";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(Long userId, String username, String tenantCode, Set<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpirationMs());
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TENANT, tenantCode)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public D2HPrincipal validateAndExtract(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        String tenantCode = claims.get(CLAIM_TENANT, String.class);

        @SuppressWarnings("unchecked")
        List<String> roleList = claims.get(CLAIM_ROLES, List.class);
        Set<String> roles = roleList != null ? Set.copyOf(roleList) : Set.of();

        return new D2HPrincipal(userId, username, tenantCode, roles);
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
