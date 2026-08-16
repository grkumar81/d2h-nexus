package org.nexus.d2h.tenant;

/**
 * Effective subscription state computed from subscriptionExpiry + gracePeriodDays.
 * Never stored in DB — derived on every access.
 */
public enum SubscriptionStatus {
    /** No expiry date set — subscription is open-ended */
    ACTIVE,
    /** Expiry set, more than 30 days away */
    ACTIVE_WITH_EXPIRY,
    /** Within 30 days of expiry */
    EXPIRY_WARNING,
    /** Past expiry but within grace period — access allowed with warning */
    GRACE_PERIOD,
    /** Past expiry and past grace period — access blocked */
    EXPIRED
}
