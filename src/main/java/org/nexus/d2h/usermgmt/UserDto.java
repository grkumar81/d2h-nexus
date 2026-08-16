package org.nexus.d2h.usermgmt;

import org.nexus.d2h.user.User;

import java.time.Instant;
import java.util.Set;

public record UserDto(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String status,
        Set<String> roles,
        Instant createdAt
) {
    static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus().name(),
                Set.copyOf(user.getRoles()),
                user.getCreatedAt()
        );
    }
}
