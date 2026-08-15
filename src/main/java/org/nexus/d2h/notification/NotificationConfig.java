package org.nexus.d2h.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.tenant.Tenant;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_config")
public class NotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Comma-separated email addresses or WhatsApp numbers */
    @Column(length = 2000)
    private String recipients;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
