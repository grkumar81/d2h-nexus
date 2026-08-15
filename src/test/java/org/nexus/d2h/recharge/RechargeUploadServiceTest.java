package org.nexus.d2h.recharge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.nexus.d2h.upload.RechargeUploadResult;
import org.nexus.d2h.upload.RechargeUploadService;
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
class RechargeUploadServiceTest {

    @Mock RechargeService rechargeService;
    @Mock RetailerRepository retailerRepository;
    @Mock AssetRepository assetRepository;
    @Mock TenantRepository tenantRepository;
    @InjectMocks RechargeUploadService rechargeUploadService;

    private Tenant tenant;
    private Retailer retailer;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantCode("T1");
        setId(tenant, 1L);

        retailer = new Retailer();
        retailer.setTenantId(1L);
        retailer.setRetailerCode("RET001");
        retailer.setRetailerName("Test Retailer");
        retailer.setMobile("9876543210");
        retailer.setStatus(RetailerStatus.ACTIVE);
        setId(retailer, 5L);

        TenantContext.setCurrentTenant("T1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of()));

        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByTenantIdAndRetailerCode(1L, "RET001")).thenReturn(Optional.of(retailer));
        when(rechargeService.createFromUpload(any(Long.class), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    RechargeTransaction tx = new RechargeTransaction();
                    tx.setAmount(inv.getArgument(5));
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
        String csv = "retailer_code,recharge_date,amount,recharge_type,reference\n" +
                     "RET001,2026-01-15,1000.00,REGULAR,RCH001\n" +
                     "RET001,2026-01-16,2000.00,MONTHLY,RCH002\n";

        RechargeUploadResult result = rechargeUploadService.upload(csvFile(csv));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(2);
        assertThat(result.failedRecords()).isEqualTo(0);
        assertThat(result.duplicateRecords()).isEqualTo(0);
        assertThat(result.totalAmountProcessed()).isEqualByComparingTo("3000.00");
    }

    @Test
    void upload_duplicateReferenceInFile_countedAsDuplicate() {
        String csv = "retailer_code,recharge_date,amount,recharge_type,reference\n" +
                     "RET001,2026-01-15,1000.00,REGULAR,RCH001\n" +
                     "RET001,2026-01-16,2000.00,REGULAR,RCH001\n";

        RechargeUploadResult result = rechargeUploadService.upload(csvFile(csv));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(1);
        assertThat(result.duplicateRecords()).isEqualTo(1);
    }

    @Test
    void upload_duplicateReferenceInDb_countedAsDuplicate() {
        when(rechargeService.createFromUpload(any(Long.class), any(), any(), any(), any(), any(),
                any(), eq("RCH001"), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException("DUPLICATE_REFERENCE", "Duplicate reference: RCH001"));

        String csv = "retailer_code,recharge_date,amount,recharge_type,reference\n" +
                     "RET001,2026-01-15,1000.00,REGULAR,RCH001\n";

        RechargeUploadResult result = rechargeUploadService.upload(csvFile(csv));

        assertThat(result.duplicateRecords()).isEqualTo(1);
        assertThat(result.successRecords()).isEqualTo(0);
    }

    @Test
    void upload_invalidRetailerCode_rowFails() {
        when(retailerRepository.findByTenantIdAndRetailerCode(1L, "BADCODE")).thenReturn(Optional.empty());

        String csv = "retailer_code,recharge_date,amount,recharge_type,reference\n" +
                     "BADCODE,2026-01-15,1000.00,REGULAR,RCH001\n";

        RechargeUploadResult result = rechargeUploadService.upload(csvFile(csv));

        assertThat(result.failedRecords()).isEqualTo(1);
        assertThat(result.errors().get(0).errorMessage()).contains("Retailer not found");
    }

    @Test
    void upload_invalidRechargeType_rowFails() {
        String csv = "retailer_code,recharge_date,amount,recharge_type,reference\n" +
                     "RET001,2026-01-15,1000.00,INVALID_TYPE,RCH001\n";

        RechargeUploadResult result = rechargeUploadService.upload(csvFile(csv));

        assertThat(result.failedRecords()).isEqualTo(1);
        assertThat(result.errors().get(0).errorMessage()).contains("Invalid recharge type");
    }

    @Test
    void upload_invalidAmount_rowFails() {
        String csv = "retailer_code,recharge_date,amount,recharge_type,reference\n" +
                     "RET001,2026-01-15,not-a-number,REGULAR,RCH001\n";

        RechargeUploadResult result = rechargeUploadService.upload(csvFile(csv));

        assertThat(result.failedRecords()).isEqualTo(1);
    }

    @Test
    void upload_missingRequiredHeader_throwsBusinessException() {
        String csv = "retailer_code,recharge_date,amount\n" +
                     "RET001,2026-01-15,1000.00\n";

        assertThatThrownBy(() -> rechargeUploadService.upload(csvFile(csv)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "MISSING_HEADERS");
    }

    @Test
    void upload_emptyFile_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> rechargeUploadService.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "EMPTY_FILE");
    }

    @Test
    void upload_partialFailure_successAndFailureCounted() {
        when(retailerRepository.findByTenantIdAndRetailerCode(1L, "BADCODE")).thenReturn(Optional.empty());

        String csv = "retailer_code,recharge_date,amount,recharge_type,reference\n" +
                     "RET001,2026-01-15,1000.00,REGULAR,RCH001\n" +
                     "BADCODE,2026-01-16,2000.00,REGULAR,RCH002\n";

        RechargeUploadResult result = rechargeUploadService.upload(csvFile(csv));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(1);
        assertThat(result.failedRecords()).isEqualTo(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "recharge.csv", "text/csv",
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
