package org.nexus.d2h.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.tenant.Tenant;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Writes an outbox event row within the caller's existing database transaction.
 * The outbox row is committed atomically with the business record.
 * Never call external providers from here.
 */
@Slf4j
@Component
public class NotificationEventPublisher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OutboxEventRepository outboxEventRepository;

    public NotificationEventPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    public void publish(Tenant tenant, NotificationEventType eventType,
                        String aggregateId, Map<String, Object> payload) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setTenant(tenant);
            event.setEventType(eventType.name());
            event.setAggregateId(aggregateId);
            event.setPayload(MAPPER.writeValueAsString(payload));
            event.setStatus(NotificationStatus.PENDING);
            outboxEventRepository.save(event);
            log.debug("Outbox event queued: type={} aggregateId={} tenant={}",
                    eventType, aggregateId, tenant.getTenantCode());
        } catch (JsonProcessingException e) {
            // Payload serialization failure must not abort the business transaction
            log.error("Failed to serialize notification payload for event={} aggregate={}: {}",
                    eventType, aggregateId, e.getMessage());
        }
    }
}
