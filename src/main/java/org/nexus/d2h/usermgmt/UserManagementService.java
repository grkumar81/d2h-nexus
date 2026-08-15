package org.nexus.d2h.usermgmt;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.audit.AuditService;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.nexus.d2h.user.User;
import org.nexus.d2h.user.UserRepository;
import org.nexus.d2h.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
public class UserManagementService {

    private static final Set<String> VALID_ROLES =
            Set.of("TENANT_ADMIN", "FINANCE_USER", "OPERATIONS_USER", "READ_ONLY");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserManagementService(UserRepository userRepository,
                                  TenantRepository tenantRepository,
                                  PasswordEncoder passwordEncoder,
                                  AuditService auditService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        Tenant tenant = resolveTenant();

        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("DUPLICATE_USERNAME",
                    "Username '" + request.username() + "' already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("DUPLICATE_EMAIL",
                    "Email '" + request.email() + "' already exists");
        }
        validateRoles(request.roles());

        User user = new User();
        user.setUsername(request.username().trim().toLowerCase());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(request.roles());
        user.getTenants().add(tenant);

        User saved = userRepository.save(user);
        log.info("User created: username={} tenant={} by={}", saved.getUsername(),
                tenant.getTenantCode(), currentUsername());
        auditService.record(tenant.getId(), "User", String.valueOf(saved.getId()),
                "CREATE", "username=" + saved.getUsername(), null);
        return UserDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listForTenant(Pageable pageable) {
        Tenant tenant = resolveTenant();
        Page<UserDto> page = userRepository.findByTenantId(tenant.getId(), pageable)
                .map(UserDto::from);
        return PageResponse.from(page);
    }

    @Transactional
    public UserDto activate(Long userId) {
        return changeStatus(userId, UserStatus.ACTIVE, "ACTIVATE");
    }

    @Transactional
    public UserDto deactivate(Long userId) {
        return changeStatus(userId, UserStatus.INACTIVE, "DEACTIVATE");
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String username = currentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        Tenant tenant = resolveTenant();
        log.info("Password changed for user={} tenant={}", username, tenant.getTenantCode());
        auditService.record(tenant.getId(), "User", String.valueOf(user.getId()),
                "PASSWORD_CHANGE", "username=" + username, null);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private UserDto changeStatus(Long userId, UserStatus newStatus, String action) {
        Tenant tenant = resolveTenant();
        User user = userRepository.findByIdAndTenantId(userId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setStatus(newStatus);
        User saved = userRepository.save(user);
        auditService.record(tenant.getId(), "User", String.valueOf(userId),
                action, "username=" + saved.getUsername(), null);
        return UserDto.from(saved);
    }

    private void validateRoles(Set<String> roles) {
        for (String role : roles) {
            if (!VALID_ROLES.contains(role)) {
                throw new BusinessException("INVALID_ROLE", "Invalid role: " + role);
            }
        }
    }

    private Tenant resolveTenant() {
        String tenantCode = TenantContext.getCurrentTenant();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
        return tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantCode));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
