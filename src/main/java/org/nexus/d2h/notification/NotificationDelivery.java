package org.nexus.d2h.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.tenant.Tenant;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_delivery")
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbox_event_id", nullable = false, updatable = false)
    private OutboxEvent outboxEvent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 500)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
