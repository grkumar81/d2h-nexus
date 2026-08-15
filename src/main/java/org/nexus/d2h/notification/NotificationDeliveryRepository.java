package org.nexus.d2h.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    Page<NotificationDelivery> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    List<NotificationDelivery> findByOutboxEventId(Long outboxEventId);
}
