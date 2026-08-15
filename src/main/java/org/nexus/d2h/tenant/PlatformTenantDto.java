package org.nexus.d2h.tenant;

import java.time.Instant;

public record PlatformTenantDto(
        Long id,
        String tenantCode,
        String name,
        String schemaName,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    static PlatformTenantDto from(Tenant t) {
        return new PlatformTenantDto(
                t.getId(), t.getTenantCode(), t.getName(),
                t.getSchemaName(), t.getStatus(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
