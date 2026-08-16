package org.nexus.d2h.tenant;

import java.time.LocalDate;

public record SubscriptionStatusDto(
        SubscriptionStatus subscriptionStatus,
        LocalDate subscriptionExpiry,
        long daysUntilExpiry,
        int gracePeriodDays,
        long graceDaysRemaining
) {
    static SubscriptionStatusDto from(SubscriptionState state) {
        return new SubscriptionStatusDto(
                state.status(),
                state.expiryDate(),
                state.daysUntilExpiry(),
                state.gracePeriodDays(),
                state.graceDaysRemaining()
        );
    }
}
