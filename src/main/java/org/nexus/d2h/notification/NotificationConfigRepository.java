package org.nexus.d2h.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {

    List<NotificationConfig> findByTenantId(Long tenantId);

    Optional<NotificationConfig> findByTenantIdAndEventTypeAndChannel(
            Long tenantId, NotificationEventType eventType, NotificationChannel channel);

    List<NotificationConfig> findByTenantIdAndEventTypeAndEnabledTrue(
            Long tenantId, NotificationEventType eventType);

    Optional<NotificationConfig> findByIdAndTenantId(Long id, Long tenantId);
}
