package org.nexus.d2h.tenant;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Computes effective subscription state from tenant fields.
 * Single place for all subscription logic — used by filter, DTOs, and API.
 */
public record SubscriptionState(
        SubscriptionStatus status,
        LocalDate expiryDate,
        long daysUntilExpiry,   // negative = days past expiry
        int gracePeriodDays,
        long graceDaysRemaining // 0 if not in grace period
) {
    private static final int WARNING_THRESHOLD_DAYS = 30;

    public static SubscriptionState compute(Tenant tenant) {
        LocalDate expiry = tenant.getSubscriptionExpiry();
        int grace = tenant.getGracePeriodDays();

        if (expiry == null) {
            return new SubscriptionState(SubscriptionStatus.ACTIVE, null, Long.MAX_VALUE, grace, 0);
        }

        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry);

        if (daysUntil > WARNING_THRESHOLD_DAYS) {
            return new SubscriptionState(SubscriptionStatus.ACTIVE_WITH_EXPIRY, expiry, daysUntil, grace, 0);
        }
        if (daysUntil >= 0) {
            return new SubscriptionState(SubscriptionStatus.EXPIRY_WARNING, expiry, daysUntil, grace, 0);
        }
        // Past expiry — check grace period
        long daysPastExpiry = -daysUntil;
        if (daysPastExpiry <= grace) {
            long graceDaysLeft = grace - daysPastExpiry;
            return new SubscriptionState(SubscriptionStatus.GRACE_PERIOD, expiry, daysUntil, grace, graceDaysLeft);
        }
        return new SubscriptionState(SubscriptionStatus.EXPIRED, expiry, daysUntil, grace, 0);
    }

    public boolean isAccessBlocked() {
        return status == SubscriptionStatus.EXPIRED;
    }

    public boolean requiresWarning() {
        return status == SubscriptionStatus.EXPIRY_WARNING || status == SubscriptionStatus.GRACE_PERIOD;
    }

    public boolean requiresNotification() {
        return status == SubscriptionStatus.GRACE_PERIOD || status == SubscriptionStatus.EXPIRED;
    }
}
