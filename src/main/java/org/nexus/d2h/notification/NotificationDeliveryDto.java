package org.nexus.d2h.notification;

import java.time.Instant;

public record NotificationDeliveryDto(
        Long id,
        Long outboxEventId,
        String eventType,
        NotificationChannel channel,
        String recipient,
        NotificationStatus status,
        int attempts,
        Instant sentAt,
        String errorMessage,
        Instant createdAt
) {
    static NotificationDeliveryDto from(NotificationDelivery d) {
        return new NotificationDeliveryDto(
                d.getId(),
                d.getOutboxEvent().getId(),
                d.getOutboxEvent().getEventType(),
                d.getChannel(),
                d.getRecipient(),
                d.getStatus(),
                d.getAttempts(),
                d.getSentAt(),
                d.getErrorMessage(),
                d.getCreatedAt());
    }
}
