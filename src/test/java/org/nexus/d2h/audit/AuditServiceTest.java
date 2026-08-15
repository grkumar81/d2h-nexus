package org.nexus.d2h.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock TenantRepository tenantRepository;
    @InjectMocks AuditService auditService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantCode("T1");
        setId(tenant, 1L);
        TenantContext.setCurrentTenant("T1");
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void record_savesAuditLog() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(tenant, "Retailer", "42", "CREATE", "code=RET001", null);

        verify(auditLogRepository).save(argThat(log ->
                log.getEntityType().equals("Retailer") &&
                log.getEntityId().equals("42") &&
                log.getAction().equals("CREATE")));
    }

    @Test
    void record_swallowsException_doesNotPropagate() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatNoException().isThrownBy(() ->
                auditService.record(tenant, "Retailer", "1", "CREATE", null, null));
    }

    @Test
    void search_returnsTenantScopedResults() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        AuditLog log = new AuditLog(tenant, "FinancialTransaction", "10", "CREATE", "user1", null, null);
        when(auditLogRepository.search(eq(1L), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        var result = auditService.search(null, null, null, null, null, null, PageRequest.of(0, 50));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).entityType()).isEqualTo("FinancialTransaction");
    }

    @Test
    void search_missingTenantContext_throwsBusinessException() {
        TenantContext.clear();

        assertThatThrownBy(() -> auditService.search(null, null, null, null, null, null, PageRequest.of(0, 50)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "TENANT_CONTEXT_MISSING");
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
