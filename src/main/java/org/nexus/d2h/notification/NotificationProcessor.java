package org.nexus.d2h.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NotificationProcessor {

    static final int MAX_ATTEMPTS = 3;
    // Backoff delays in seconds: attempt 1→60s, attempt 2→300s, attempt 3→900s
    private static final long[] BACKOFF_SECONDS = {60L, 300L, 900L};

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationConfigRepository configRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationTemplateService templateService;
    private final EmailProvider emailProvider;
    private final WhatsAppProvider whatsAppProvider;

    public NotificationProcessor(OutboxEventRepository outboxEventRepository,
                                  NotificationConfigRepository configRepository,
                                  NotificationDeliveryRepository deliveryRepository,
                                  NotificationTemplateService templateService,
                                  EmailProvider emailProvider,
                                  WhatsAppProvider whatsAppProvider) {
        this.outboxEventRepository = outboxEventRepository;
        this.configRepository = configRepository;
        this.deliveryRepository = deliveryRepository;
        this.templateService = templateService;
        this.emailProvider = emailProvider;
        this.whatsAppProvider = whatsAppProvider;
    }

    @Scheduled(fixedDelayString = "${app.notification.poll-interval-ms:30000}")
    public void process() {
        List<OutboxEvent> due = outboxEventRepository.findDueForProcessing(Instant.now());
        if (due.isEmpty()) return;
        log.debug("Processing {} outbox event(s)", due.size());
        for (OutboxEvent event : due) {
            processEvent(event);
        }
    }

    @Transactional
    public void processEvent(OutboxEvent event) {
        event.setStatus(NotificationStatus.PROCESSING);
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);

        NotificationEventType eventType;
        try {
            eventType = NotificationEventType.valueOf(event.getEventType());
        } catch (IllegalArgumentException e) {
            markFailed(event, "Unknown event type: " + event.getEventType());
            return;
        }

        Map<String, Object> payload = templateService.parsePayload(event.getPayload());
        Long tenantId = event.getTenantId();

        List<NotificationConfig> configs =
                configRepository.findByTenantIdAndEventTypeAndEnabledTrue(tenantId, eventType);

        if (configs.isEmpty()) {
            // No configuration — mark processed silently
            markProcessed(event);
            return;
        }

        boolean anyFailure = false;
        for (NotificationConfig config : configs) {
            if (config.getRecipients() == null || config.getRecipients().isBlank()) continue;
            String[] recipients = config.getRecipients().split(",");
            for (String recipient : recipients) {
                recipient = recipient.trim();
                if (recipient.isBlank()) continue;
                boolean sent = dispatch(event, eventType, config.getChannel(), recipient, payload);
                if (!sent) anyFailure = true;
            }
        }

        event.setAttempts(event.getAttempts() + 1);
        if (anyFailure && event.getAttempts() < MAX_ATTEMPTS) {
            long delay = BACKOFF_SECONDS[Math.min(event.getAttempts(), BACKOFF_SECONDS.length - 1)];
            event.setStatus(NotificationStatus.RETRYING);
            event.setNextRetryAt(Instant.now().plusSeconds(delay));
            log.info("Outbox event {} scheduled for retry attempt={} in {}s",
                    event.getId(), event.getAttempts(), delay);
        } else if (anyFailure) {
            markFailed(event, "Max attempts reached");
        } else {
            markProcessed(event);
        }
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean dispatch(OutboxEvent event, NotificationEventType eventType,
                              NotificationChannel channel, String recipient,
                              Map<String, Object> payload) {
        NotificationDelivery delivery = findOrCreateDelivery(event, channel, recipient);
        delivery.setAttempts(delivery.getAttempts() + 1);
        try {
            if (channel == NotificationChannel.EMAIL) {
                String subject = templateService.buildSubject(eventType, payload);
                String body = templateService.buildEmailBody(eventType, payload);
                emailProvider.send(recipient, subject, body);
            } else {
                String message = templateService.buildWhatsAppMessage(eventType, payload);
                whatsAppProvider.send(recipient, message);
            }
            delivery.setStatus(NotificationStatus.SENT);
            delivery.setSentAt(Instant.now());
            delivery.setErrorMessage(null);
            delivery.setUpdatedAt(Instant.now());
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            log.warn("Notification delivery failed: channel={} recipient={} event={} error={}",
                    channel, recipient, event.getId(), e.getMessage());
            delivery.setStatus(NotificationStatus.FAILED);
            delivery.setErrorMessage(truncate(e.getMessage(), 1000));
            delivery.setUpdatedAt(Instant.now());
            deliveryRepository.save(delivery);
            return false;
        }
    }

    private NotificationDelivery findOrCreateDelivery(OutboxEvent event,
                                                        NotificationChannel channel,
                                                        String recipient) {
        return deliveryRepository.findByOutboxEventId(event.getId()).stream()
                .filter(d -> d.getChannel() == channel && d.getRecipient().equals(recipient))
                .findFirst()
                .orElseGet(() -> {
                    NotificationDelivery d = new NotificationDelivery();
                    d.setTenantId(event.getTenantId());
                    d.setOutboxEvent(event);
                    d.setChannel(channel);
                    d.setRecipient(recipient);
                    d.setStatus(NotificationStatus.PENDING);
                    return d;
                });
    }

    private void markProcessed(OutboxEvent event) {
        event.setStatus(NotificationStatus.SENT);
        event.setProcessedAt(Instant.now());
    }

    private void markFailed(OutboxEvent event, String reason) {
        event.setStatus(NotificationStatus.FAILED);
        event.setErrorMessage(truncate(reason, 1000));
        event.setProcessedAt(Instant.now());
        log.warn("Outbox event {} permanently failed: {}", event.getId(), reason);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
