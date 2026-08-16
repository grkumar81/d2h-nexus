package org.nexus.d2h.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces subscription expiry on every business API request.
 * Runs after TenantContextFilter (Order 3) so tenantCode is already set.
 * Uses lazy evaluation — no scheduler, no DB status writes.
 *
 * Grace period  → allows request, adds X-Subscription-Warning header
 * Expired       → rejects with 403 SUBSCRIPTION_EXPIRED
 * PLATFORM_ADMIN (null tenantCode) → skipped
 */
@Slf4j
@Component
@Order(4)
public class SubscriptionFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    public SubscriptionFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String tenantCode = TenantContext.getCurrentTenant();

        if (tenantCode == null || tenantCode.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Tenant tenant = tenantRepository.findByTenantCode(tenantCode).orElse(null);
        if (tenant == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SubscriptionState state = SubscriptionState.compute(tenant);

        if (state.isAccessBlocked()) {
            log.warn("Subscription expired — access blocked: tenant={}", tenantCode);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"SUBSCRIPTION_EXPIRED\",\"message\":\"Subscription has expired. Please contact your administrator.\"}");
            return;
        }

        if (state.requiresWarning()) {
            response.setHeader("X-Subscription-Warning", buildWarningMessage(state));
        }

        filterChain.doFilter(request, response);
    }

    private String buildWarningMessage(SubscriptionState state) {
        return switch (state.status()) {
            case EXPIRY_WARNING -> "Subscription expires in " + state.daysUntilExpiry() + " day(s)";
            case GRACE_PERIOD   -> "Subscription expired. Grace period: " + state.graceDaysRemaining() + " day(s) remaining";
            default             -> "";
        };
    }
}
