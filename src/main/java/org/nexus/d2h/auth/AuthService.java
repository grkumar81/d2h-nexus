package org.nexus.d2h.auth;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.notification.NotificationEventPublisher;
import org.nexus.d2h.notification.NotificationEventType;
import org.nexus.d2h.tenant.SubscriptionState;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantRepository;
import org.nexus.d2h.user.User;
import org.nexus.d2h.user.UserRepository;
import org.nexus.d2h.user.UserStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 900; // 15 minutes

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TenantRepository tenantRepository;
    private final NotificationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       TenantRepository tenantRepository,
                       NotificationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new DisabledException("Account is disabled");
        }

        if (isLocked(user)) {
            throw new LockedException("Account is temporarily locked due to too many failed login attempts");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedAttempt(user);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Successful login — reset failed attempts
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // PLATFORM_ADMIN users have no tenant association — tenantCode is null in their JWT.
        // Regular users resolve tenantCode from their first associated tenant.
        boolean isPlatformAdmin = user.getRoles().contains("PLATFORM_ADMIN");
        String tenantCode = isPlatformAdmin ? null
                : user.getTenants().stream().findFirst().map(t -> t.getTenantCode()).orElse(null);

        String token = jwtService.generateToken(user.getId(), user.getUsername(), tenantCode, user.getRoles());
        log.info("User '{}' logged in: tenantCode={} roles={}", user.getUsername(), tenantCode, user.getRoles());

        // Fire subscription expiry notification once per 24h if in grace/expired state
        if (!isPlatformAdmin && tenantCode != null) {
            fireExpiryNotificationIfNeeded(tenantCode);
        }

        return new LoginResponse(token, user.getUsername(), tenantCode, user.getRoles(), jwtProperties.getExpirationMs());
    }

    private void fireExpiryNotificationIfNeeded(String tenantCode) {
        try {
            Tenant tenant = tenantRepository.findByTenantCode(tenantCode).orElse(null);
            if (tenant == null) return;
            SubscriptionState state = SubscriptionState.compute(tenant);
            if (!state.requiresNotification()) return;
            // Throttle — only notify once per 24 hours
            if (tenant.getLastExpiryNotifiedAt() != null &&
                    ChronoUnit.HOURS.between(tenant.getLastExpiryNotifiedAt(), Instant.now()) < 24) return;
            tenant.setLastExpiryNotifiedAt(Instant.now());
            tenantRepository.save(tenant);
            eventPublisher.publish(NotificationEventType.SUBSCRIPTION_EXPIRY_WARNING,
                    tenant.getTenantCode(),
                    Map.of("tenantName", tenant.getName(),
                           "subscriptionStatus", state.status().name(),
                           "daysUntilExpiry", state.daysUntilExpiry(),
                           "graceDaysRemaining", state.graceDaysRemaining()));
        } catch (Exception e) {
            log.warn("Failed to fire expiry notification for tenant={}: {}", tenantCode, e.getMessage());
        }
    }

    private boolean isLocked(User user) {
        if (user.getStatus() == UserStatus.LOCKED) return true;
        if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) return true;
        return false;
    }

    private void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            log.warn("User '{}' locked after {} failed login attempts", user.getUsername(), attempts);
        }
        userRepository.save(user);
    }
}
