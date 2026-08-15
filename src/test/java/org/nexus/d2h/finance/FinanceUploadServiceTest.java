package org.nexus.d2h.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.notification.NotificationEventPublisher;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.nexus.d2h.upload.FinanceUploadResult;
import org.nexus.d2h.upload.FinanceUploadService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceUploadServiceTest {

    @Mock FinanceService financeService;
    @Mock RetailerRepository retailerRepository;
    @Mock TenantRepository tenantRepository;
    @Mock NotificationEventPublisher eventPublisher;
    @InjectMocks FinanceUploadService financeUploadService;

    private Retailer retailer;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setTenantCode("T1");
        setId(tenant, 1L);

        retailer = new Retailer();
        retailer.setRetailerCode("RET001");
        retailer.setRetailerName("Test Retailer");
        retailer.setMobile("9876543210");
        setId(retailer, 5L);

        TenantContext.setCurrentTenant("T1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of()));

        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByRetailerCode("RET001")).thenReturn(Optional.of(retailer));
        when(financeService.createFromUpload(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    var tx = new FinancialTransaction();
                    tx.setAmount(inv.getArgument(3));
                    return tx;
                });
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void upload_validCsv_allSuccess() {
        String csv = "retailer_code,transaction_date,transaction_type,amount,reference\n" +
                     "RET001,2025-01-15,PAYMENT_RECEIVED,5000.00,REF001\n" +
                     "RET001,2025-01-16,PAYMENT_RECEIVED,3000.00,REF002\n";

        FinanceUploadResult result = financeUploadService.upload(csvFile(csv));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(2);
        assertThat(result.failedRecords()).isEqualTo(0);
        assertThat(result.duplicateRecords()).isEqualTo(0);
        assertThat(result.totalAmountProcessed()).isEqualByComparingTo("8000.00");
    }

    @Test
    void upload_duplicateReferenceInFile_countedAsDuplicate() {
        String csv = "retailer_code,transaction_date,transaction_type,amount,reference\n" +
                     "RET001,2025-01-15,PAYMENT_RECEIVED,5000.00,REF001\n" +
                     "RET001,2025-01-16,PAYMENT_RECEIVED,3000.00,REF001\n";

        FinanceUploadResult result = financeUploadService.upload(csvFile(csv));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(1);
        assertThat(result.duplicateRecords()).isEqualTo(1);
    }

    @Test
    void upload_duplicateReferenceInDb_countedAsDuplicate() {
        when(financeService.createFromUpload(any(), any(), any(), any(), any(), eq("REF001"), any(), any(), any()))
                .thenThrow(new BusinessException("DUPLICATE_REFERENCE", "Duplicate reference: REF001"));

        String csv = "retailer_code,transaction_date,transaction_type,amount,reference\n" +
                     "RET001,2025-01-15,PAYMENT_RECEIVED,5000.00,REF001\n";

        FinanceUploadResult result = financeUploadService.upload(csvFile(csv));

        assertThat(result.duplicateRecords()).isEqualTo(1);
        assertThat(result.successRecords()).isEqualTo(0);
    }

    @Test
    void upload_invalidRetailerCode_rowFails() {
        when(retailerRepository.findByRetailerCode("BADCODE")).thenReturn(Optional.empty());

        String csv = "retailer_code,transaction_date,transaction_type,amount,reference\n" +
                     "BADCODE,2025-01-15,PAYMENT_RECEIVED,5000.00,REF001\n";

        FinanceUploadResult result = financeUploadService.upload(csvFile(csv));

        assertThat(result.failedRecords()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorMessage()).contains("Retailer not found");
    }

    @Test
    void upload_invalidTransactionType_rowFails() {
        String csv = "retailer_code,transaction_date,transaction_type,amount,reference\n" +
                     "RET001,2025-01-15,INVALID_TYPE,5000.00,REF001\n";

        FinanceUploadResult result = financeUploadService.upload(csvFile(csv));

        assertThat(result.failedRecords()).isEqualTo(1);
        assertThat(result.errors().get(0).errorMessage()).contains("Invalid transaction type");
    }

    @Test
    void upload_invalidAmount_rowFails() {
        String csv = "retailer_code,transaction_date,transaction_type,amount,reference\n" +
                     "RET001,2025-01-15,PAYMENT_RECEIVED,not-a-number,REF001\n";

        FinanceUploadResult result = financeUploadService.upload(csvFile(csv));

        assertThat(result.failedRecords()).isEqualTo(1);
    }

    @Test
    void upload_missingRequiredHeader_throwsBusinessException() {
        String csv = "retailer_code,transaction_date,amount\nRET001,2025-01-15,5000.00\n";

        assertThatThrownBy(() -> financeUploadService.upload(csvFile(csv)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "MISSING_HEADERS");
    }

    @Test
    void upload_emptyFile_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> financeUploadService.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "EMPTY_FILE");
    }

    @Test
    void upload_partialFailure_successAndFailureCounted() {
        when(retailerRepository.findByRetailerCode("BADCODE")).thenReturn(Optional.empty());

        String csv = "retailer_code,transaction_date,transaction_type,amount,reference\n" +
                     "RET001,2025-01-15,PAYMENT_RECEIVED,5000.00,REF001\n" +
                     "BADCODE,2025-01-16,PAYMENT_RECEIVED,3000.00,REF002\n";

        FinanceUploadResult result = financeUploadService.upload(csvFile(csv));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(1);
        assertThat(result.failedRecords()).isEqualTo(1);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "finance.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
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
