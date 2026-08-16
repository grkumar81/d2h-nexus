package org.nexus.d2h.usermgmt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Size(max = 255) String fullName,

        @Pattern(regexp = "^[+\\d\\s\\-()]{7,30}$", message = "Invalid phone number")
        String phone,

        @NotEmpty Set<String> roles
) {}
