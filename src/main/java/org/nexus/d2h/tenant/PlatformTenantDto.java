package org.nexus.d2h.tenant;

import java.time.Instant;
import java.time.LocalDate;

public record PlatformTenantDto(
        Long id,
        String tenantCode,
        String name,
        String email,
        String phone,
        String schemaName,
        TenantStatus status,
        SubscriptionStatus subscriptionStatus,
        LocalDate subscriptionExpiry,
        long daysUntilExpiry,
        int gracePeriodDays,
        long graceDaysRemaining,
        Instant createdAt,
        Instant updatedAt
) {
    static PlatformTenantDto from(Tenant t) {
        SubscriptionState sub = SubscriptionState.compute(t);
        return new PlatformTenantDto(
                t.getId(), t.getTenantCode(), t.getName(),
                t.getEmail(), t.getPhone(),
                t.getSchemaName(), t.getStatus(),
                sub.status(), sub.expiryDate(), sub.daysUntilExpiry(),
                sub.gracePeriodDays(), sub.graceDaysRemaining(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
