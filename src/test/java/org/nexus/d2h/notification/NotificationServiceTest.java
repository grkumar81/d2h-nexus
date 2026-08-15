package org.nexus.d2h.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationConfigRepository configRepository;
    @Mock NotificationDeliveryRepository deliveryRepository;
    @Mock TenantRepository tenantRepository;
    @InjectMocks NotificationService notificationService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantCode("T1");
        setId(tenant, 1L);
        TenantContext.setCurrentTenant("T1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── listConfigs ───────────────────────────────────────────────────────────

    @Test
    void listConfigs_returnsTenantConfigs() {
        NotificationConfig config = configWithId(10L, NotificationEventType.FINANCE_TRANSACTION_CREATED,
                NotificationChannel.EMAIL);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(configRepository.findByTenantId(1L)).thenReturn(List.of(config));

        List<NotificationConfigDto> result = notificationService.listConfigs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventType()).isEqualTo(NotificationEventType.FINANCE_TRANSACTION_CREATED);
        assertThat(result.get(0).channel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void listConfigs_noTenantContext_throwsBusinessException() {
        TenantContext.clear();
        assertThatThrownBy(() -> notificationService.listConfigs())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "TENANT_CONTEXT_MISSING");
    }

    // ── saveConfig ────────────────────────────────────────────────────────────

    @Test
    void saveConfig_createsNewConfig() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(configRepository.findByTenantIdAndEventTypeAndChannel(1L,
                NotificationEventType.FINANCE_TRANSACTION_CREATED, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
        when(configRepository.save(any())).thenAnswer(inv -> {
            setId(inv.getArgument(0), 20L);
            return inv.getArgument(0);
        });

        NotificationConfigDto dto = notificationService.saveConfig(new SaveNotificationConfigRequest(
                NotificationEventType.FINANCE_TRANSACTION_CREATED, NotificationChannel.EMAIL,
                true, "test@example.com"));

        assertThat(dto.enabled()).isTrue();
        assertThat(dto.recipients()).isEqualTo("test@example.com");
        verify(configRepository).save(any());
    }

    @Test
    void saveConfig_updatesExistingConfig() {
        NotificationConfig existing = configWithId(10L, NotificationEventType.FINANCE_TRANSACTION_CREATED,
                NotificationChannel.EMAIL);
        existing.setEnabled(false);
        existing.setRecipients("old@example.com");

        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(configRepository.findByTenantIdAndEventTypeAndChannel(1L,
                NotificationEventType.FINANCE_TRANSACTION_CREATED, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(existing));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationConfigDto dto = notificationService.saveConfig(new SaveNotificationConfigRequest(
                NotificationEventType.FINANCE_TRANSACTION_CREATED, NotificationChannel.EMAIL,
                true, "new@example.com"));

        assertThat(dto.enabled()).isTrue();
        assertThat(dto.recipients()).isEqualTo("new@example.com");
    }

    // ── deleteConfig ──────────────────────────────────────────────────────────

    @Test
    void deleteConfig_existingConfig_deletes() {
        NotificationConfig config = configWithId(10L, NotificationEventType.FINANCE_TRANSACTION_CREATED,
                NotificationChannel.EMAIL);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(configRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(config));

        notificationService.deleteConfig(10L);

        verify(configRepository).delete(config);
    }

    @Test
    void deleteConfig_notFound_throwsResourceNotFoundException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(configRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteConfig(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteConfig_tenantIsolation_cannotDeleteOtherTenantConfig() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        // findByIdAndTenantId scopes to tenant — returns empty for cross-tenant access
        when(configRepository.findByIdAndTenantId(50L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteConfig(50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── listDeliveries ────────────────────────────────────────────────────────

    @Test
    void listDeliveries_returnsPaginatedResults() {
        NotificationDelivery delivery = deliveryWithId(1L);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(deliveryRepository.findByTenantIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        var result = notificationService.listDeliveries(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private NotificationConfig configWithId(Long id, NotificationEventType eventType,
                                             NotificationChannel channel) {
        NotificationConfig c = new NotificationConfig();
        c.setTenantId(1L);
        c.setEventType(eventType);
        c.setChannel(channel);
        c.setEnabled(true);
        c.setRecipients("test@example.com");
        setId(c, id);
        return c;
    }

    private NotificationDelivery deliveryWithId(Long id) {
        OutboxEvent event = new OutboxEvent();
        event.setTenantId(1L);
        event.setEventType(NotificationEventType.FINANCE_TRANSACTION_CREATED.name());
        event.setAggregateId("1");
        event.setPayload("{}");
        setId(event, 100L);

        NotificationDelivery d = new NotificationDelivery();
        d.setTenantId(1L);
        d.setOutboxEvent(event);
        d.setChannel(NotificationChannel.EMAIL);
        d.setRecipient("test@example.com");
        d.setStatus(NotificationStatus.SENT);
        setId(d, id);
        return d;
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
