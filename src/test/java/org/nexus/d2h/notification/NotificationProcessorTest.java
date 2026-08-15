package org.nexus.d2h.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock NotificationConfigRepository configRepository;
    @Mock NotificationDeliveryRepository deliveryRepository;
    @Mock NotificationTemplateService templateService;
    @Mock EmailProvider emailProvider;
    @Mock WhatsAppProvider whatsAppProvider;
    @InjectMocks NotificationProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(templateService.parsePayload(any())).thenReturn(java.util.Map.of());
    }

    @Test
    void processEvent_noConfig_marksProcessedSilently() {
        OutboxEvent event = pendingEvent(1L, NotificationEventType.FINANCE_TRANSACTION_CREATED);
        when(configRepository.findByEventTypeAndEnabledTrue(NotificationEventType.FINANCE_TRANSACTION_CREATED))
                .thenReturn(List.of());
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.processEvent(event);

        assertThat(event.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(event.getProcessedAt()).isNotNull();
        verifyNoInteractions(emailProvider, whatsAppProvider);
    }

    @Test
    void processEvent_emailConfig_sendsEmail() {
        OutboxEvent event = pendingEvent(1L, NotificationEventType.FINANCE_TRANSACTION_CREATED);
        NotificationConfig config = emailConfig(NotificationEventType.FINANCE_TRANSACTION_CREATED, "test@example.com");
        when(configRepository.findByEventTypeAndEnabledTrue(NotificationEventType.FINANCE_TRANSACTION_CREATED))
                .thenReturn(List.of(config));
        when(deliveryRepository.findByOutboxEventId(1L)).thenReturn(List.of());
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateService.buildSubject(any(), any())).thenReturn("Subject");
        when(templateService.buildEmailBody(any(), any())).thenReturn("Body");

        processor.processEvent(event);

        verify(emailProvider).send("test@example.com", "Subject", "Body");
        assertThat(event.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void processEvent_whatsappConfig_sendsWhatsApp() {
        OutboxEvent event = pendingEvent(2L, NotificationEventType.RECHARGE_CREATED);
        NotificationConfig config = whatsappConfig(NotificationEventType.RECHARGE_CREATED, "+919876543210");
        when(configRepository.findByEventTypeAndEnabledTrue(NotificationEventType.RECHARGE_CREATED))
                .thenReturn(List.of(config));
        when(deliveryRepository.findByOutboxEventId(2L)).thenReturn(List.of());
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateService.buildWhatsAppMessage(any(), any())).thenReturn("WA message");

        processor.processEvent(event);

        verify(whatsAppProvider).send("+919876543210", "WA message");
        assertThat(event.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void processEvent_emailFails_schedulesRetry() {
        OutboxEvent event = pendingEvent(3L, NotificationEventType.FINANCE_TRANSACTION_CREATED);
        NotificationConfig config = emailConfig(NotificationEventType.FINANCE_TRANSACTION_CREATED, "fail@example.com");
        when(configRepository.findByEventTypeAndEnabledTrue(NotificationEventType.FINANCE_TRANSACTION_CREATED))
                .thenReturn(List.of(config));
        when(deliveryRepository.findByOutboxEventId(3L)).thenReturn(List.of());
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateService.buildSubject(any(), any())).thenReturn("Subject");
        when(templateService.buildEmailBody(any(), any())).thenReturn("Body");
        doThrow(new RuntimeException("SMTP error")).when(emailProvider).send(any(), any(), any());

        processor.processEvent(event);

        assertThat(event.getStatus()).isEqualTo(NotificationStatus.RETRYING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isAfter(Instant.now());
    }

    @Test
    void processEvent_maxAttemptsReached_marksFailed() {
        OutboxEvent event = pendingEvent(4L, NotificationEventType.FINANCE_TRANSACTION_CREATED);
        event.setAttempts(NotificationProcessor.MAX_ATTEMPTS - 1);
        NotificationConfig config = emailConfig(NotificationEventType.FINANCE_TRANSACTION_CREATED, "fail@example.com");
        when(configRepository.findByEventTypeAndEnabledTrue(NotificationEventType.FINANCE_TRANSACTION_CREATED))
                .thenReturn(List.of(config));
        when(deliveryRepository.findByOutboxEventId(4L)).thenReturn(List.of());
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateService.buildSubject(any(), any())).thenReturn("Subject");
        when(templateService.buildEmailBody(any(), any())).thenReturn("Body");
        doThrow(new RuntimeException("SMTP error")).when(emailProvider).send(any(), any(), any());

        processor.processEvent(event);

        assertThat(event.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void processEvent_unknownEventType_marksFailed() {
        OutboxEvent event = pendingEvent(5L, NotificationEventType.FINANCE_TRANSACTION_CREATED);
        event.setEventType("UNKNOWN_TYPE");
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.processEvent(event);

        assertThat(event.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Unknown event type");
    }

    @Test
    void processEvent_multipleRecipients_sendsToAll() {
        OutboxEvent event = pendingEvent(6L, NotificationEventType.FINANCE_TRANSACTION_CREATED);
        NotificationConfig config = emailConfig(NotificationEventType.FINANCE_TRANSACTION_CREATED, "a@example.com,b@example.com");
        when(configRepository.findByEventTypeAndEnabledTrue(NotificationEventType.FINANCE_TRANSACTION_CREATED))
                .thenReturn(List.of(config));
        when(deliveryRepository.findByOutboxEventId(6L)).thenReturn(List.of());
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateService.buildSubject(any(), any())).thenReturn("Subject");
        when(templateService.buildEmailBody(any(), any())).thenReturn("Body");

        processor.processEvent(event);

        verify(emailProvider).send(eq("a@example.com"), any(), any());
        verify(emailProvider).send(eq("b@example.com"), any(), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OutboxEvent pendingEvent(Long id, NotificationEventType type) {
        OutboxEvent e = new OutboxEvent();
        e.setEventType(type.name());
        e.setAggregateId("1");
        e.setPayload("{}");
        e.setStatus(NotificationStatus.PENDING);
        setId(e, id);
        return e;
    }

    private NotificationConfig emailConfig(NotificationEventType type, String recipients) {
        NotificationConfig c = new NotificationConfig();
        c.setEventType(type);
        c.setChannel(NotificationChannel.EMAIL);
        c.setEnabled(true);
        c.setRecipients(recipients);
        setId(c, 10L);
        return c;
    }

    private NotificationConfig whatsappConfig(NotificationEventType type, String recipients) {
        NotificationConfig c = new NotificationConfig();
        c.setEventType(type);
        c.setChannel(NotificationChannel.WHATSAPP);
        c.setEnabled(true);
        c.setRecipients(recipients);
        setId(c, 11L);
        return c;
    }

    private void setId(Object entity, Long id) {
        try {
            Class<?> cls = entity.getClass();
            java.lang.reflect.Field field = null;
            while (cls != null) {
                try { field = cls.getDeclaredField("id"); break; }
                catch (NoSuchFieldException e) { cls = cls.getSuperclass(); }
            }
            if (field == null) throw new NoSuchFieldException("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
