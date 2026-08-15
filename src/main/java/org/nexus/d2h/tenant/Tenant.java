package org.nexus.d2h.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(name = "tenant_code", nullable = false, unique = true, length = 50)
    private String tenantCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "schema_name", nullable = false, unique = true, length = 64)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantStatus status = TenantStatus.PENDING;
}
