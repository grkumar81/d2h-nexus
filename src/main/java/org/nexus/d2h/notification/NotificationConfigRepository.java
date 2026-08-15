package org.nexus.d2h.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {

    List<NotificationConfig> findAll();

    Optional<NotificationConfig> findByEventTypeAndChannel(
            NotificationEventType eventType, NotificationChannel channel);

    List<NotificationConfig> findByEventTypeAndEnabledTrue(NotificationEventType eventType);
}
