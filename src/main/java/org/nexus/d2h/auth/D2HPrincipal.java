package org.nexus.d2h.auth;

import java.util.Set;

/**
 * Immutable principal stored in the SecurityContext after JWT validation.
 * tenantCode is resolved from JWT claims — never from the browser request.
 */
public record D2HPrincipal(Long userId, String username, String tenantCode, Set<String> roles) {}
