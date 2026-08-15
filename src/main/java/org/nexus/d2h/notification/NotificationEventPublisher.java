package org.nexus.d2h.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class NotificationEventPublisher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OutboxEventRepository outboxEventRepository;

    public NotificationEventPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    public void publish(Long tenantId, NotificationEventType eventType,
                        String aggregateId, Map<String, Object> payload) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setTenantId(tenantId);
            event.setEventType(eventType.name());
            event.setAggregateId(aggregateId);
            event.setPayload(MAPPER.writeValueAsString(payload));
            event.setStatus(NotificationStatus.PENDING);
            outboxEventRepository.save(event);
            log.debug("Outbox event queued: type={} aggregateId={} tenantId={}",
                    eventType, aggregateId, tenantId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification payload for event={} aggregate={}: {}",
                    eventType, aggregateId, e.getMessage());
        }
    }
}
