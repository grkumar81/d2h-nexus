package org.nexus.d2h.tenant;

public record TenantProfileDto(
        String tenantCode,
        String name,
        String email,
        String phone,
        String status
) {
    static TenantProfileDto from(Tenant t) {
        return new TenantProfileDto(
                t.getTenantCode(), t.getName(),
                t.getEmail(), t.getPhone(),
                t.getStatus().name()
        );
    }
}
