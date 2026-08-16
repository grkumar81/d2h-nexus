package org.nexus.d2h.usermgmt;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 255) String fullName,
        @Pattern(regexp = "^[+\\d\\s\\-()]{7,30}$", message = "Invalid phone number") String phone,
        @NotEmpty Set<String> roles
) {}
