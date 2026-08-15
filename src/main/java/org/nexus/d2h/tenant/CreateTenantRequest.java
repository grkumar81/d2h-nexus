package org.nexus.d2h.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(min = 2, max = 50)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "tenantCode must be uppercase alphanumeric with underscores")
        String tenantCode,

        @NotBlank @Size(max = 255)
        String name
) {}
