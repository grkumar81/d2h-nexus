package org.nexus.d2h.tenant;

/**
 * Holds the current tenant identifier for the duration of a request.
 * Populated by TenantContextFilter from the authenticated JWT claims.
 * Must be cleared after every request to prevent thread-pool leakage.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(String tenantCode) {
        CURRENT_TENANT.set(tenantCode);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
