package org.nexus.d2h.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @InjectMocks AuditService auditService;

    @Test
    void record_savesAuditLog() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.record("Retailer", "42", "CREATE", "code=RET001", null);

        verify(auditLogRepository).save(argThat(log ->
                log.getEntityType().equals("Retailer") &&
                log.getEntityId().equals("42") &&
                log.getAction().equals("CREATE")));
    }

    @Test
    void record_swallowsException_doesNotPropagate() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatNoException().isThrownBy(() ->
                auditService.record("Retailer", "1", "CREATE", null, null));
    }

    @Test
    void search_returnsPaginatedResults() {
        AuditLog log = new AuditLog("FinancialTransaction", "10", "CREATE", "user1", null, null);
        when(auditLogRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        var result = auditService.search(null, null, null, null, null, null, PageRequest.of(0, 50));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).entityType()).isEqualTo("FinancialTransaction");
    }
}
