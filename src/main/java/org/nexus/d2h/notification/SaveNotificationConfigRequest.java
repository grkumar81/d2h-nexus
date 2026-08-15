package org.nexus.d2h.notification;

import jakarta.validation.constraints.NotNull;

public record SaveNotificationConfigRequest(
        @NotNull NotificationEventType eventType,
        @NotNull NotificationChannel channel,
        boolean enabled,
        String recipients
) {}
