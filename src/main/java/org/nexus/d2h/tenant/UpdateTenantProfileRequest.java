package org.nexus.d2h.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTenantProfileRequest(
        @NotBlank @Size(max = 255)
        String name,

        @Email @Size(max = 255)
        String email,

        @Pattern(regexp = "^[+\\d\\s\\-()]{7,30}$", message = "Invalid phone number")
        String phone
) {}
