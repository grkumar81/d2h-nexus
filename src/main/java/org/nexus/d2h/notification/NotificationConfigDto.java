package org.nexus.d2h.notification;

import java.time.Instant;

public record NotificationConfigDto(
        Long id,
        NotificationEventType eventType,
        NotificationChannel channel,
        boolean enabled,
        String recipients,
        Instant updatedAt
) {
    static NotificationConfigDto from(NotificationConfig c) {
        return new NotificationConfigDto(
                c.getId(), c.getEventType(), c.getChannel(),
                c.isEnabled(), c.getRecipients(), c.getUpdatedAt());
    }
}
