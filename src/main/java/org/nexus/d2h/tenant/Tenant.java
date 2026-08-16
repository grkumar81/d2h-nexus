package org.nexus.d2h.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.common.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(name = "tenant_code", nullable = false, unique = true, length = 50)
    private String tenantCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "subscription_expiry")
    private java.time.LocalDate subscriptionExpiry;

    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays = 30;

    @Column(name = "last_expiry_notified_at")
    private Instant lastExpiryNotifiedAt;

    @Column(name = "schema_name", nullable = false, unique = true, length = 64)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantStatus status = TenantStatus.PENDING;
}
