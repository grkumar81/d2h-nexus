package org.nexus.d2h.auth;

import java.util.Set;

public record LoginResponse(
        String token,
        String username,
        String tenantCode,
        Set<String> roles,
        long expiresInMs
) {}
