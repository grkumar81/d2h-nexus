package org.nexus.d2h.retailer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.nexus.d2h.upload.RetailerUploadService;
import org.nexus.d2h.upload.UploadResult;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetailerUploadServiceTest {

    @Mock RetailerRepository retailerRepository;
    @Mock TenantRepository tenantRepository;
    @InjectMocks RetailerUploadService uploadService;

    private Tenant tenant;

    @BeforeEach
    void setUp() throws Exception {
        tenant = new Tenant();
        tenant.setTenantCode("TENANT1");
        var idField = org.nexus.d2h.common.BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(tenant, 1L);
        TenantContext.setCurrentTenant("TENANT1");
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void validCsv_allRowsInserted() {
        String csv = "retailer_code,retailer_name,mobile\nRET001,Retailer One,9876543210\nRET002,Retailer Two,9876543211\n";
        MockMultipartFile file = csvFile(csv);

        when(retailerRepository.existsByTenantIdAndRetailerCode(eq(1L), anyString())).thenReturn(false);
        when(retailerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        UploadResult result = uploadService.upload(file);

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(2);
        assertThat(result.failedRecords()).isEqualTo(0);
        assertThat(result.duplicateRecords()).isEqualTo(0);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void csvWithDuplicateInFile_countedAsDuplicate() {
        String csv = "retailer_code,retailer_name,mobile\nRET001,Retailer One,9876543210\nRET001,Retailer One Again,9876543211\n";
        MockMultipartFile file = csvFile(csv);

        when(retailerRepository.existsByTenantIdAndRetailerCode(eq(1L), anyString())).thenReturn(false);
        when(retailerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        UploadResult result = uploadService.upload(file);

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successRecords()).isEqualTo(1);
        assertThat(result.duplicateRecords()).isEqualTo(1);
    }

    @Test
    void csvWithExistingCode_countedAsDuplicate() {
        String csv = "retailer_code,retailer_name,mobile\nRET001,Retailer One,9876543210\n";
        MockMultipartFile file = csvFile(csv);

        when(retailerRepository.existsByTenantIdAndRetailerCode(1L, "RET001")).thenReturn(true);

        UploadResult result = uploadService.upload(file);

        assertThat(result.duplicateRecords()).isEqualTo(1);
        assertThat(result.successRecords()).isEqualTo(0);
    }

    @Test
    void csvMissingRequiredColumn_throwsBusinessException() {
        String csv = "retailer_code,retailer_name\nRET001,Retailer One\n";
        MockMultipartFile file = csvFile(csv);

        assertThatThrownBy(() -> uploadService.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mobile");
    }

    @Test
    void csvInvalidMobile_rowFails() {
        String csv = "retailer_code,retailer_name,mobile\nRET001,Retailer One,INVALID\n";
        MockMultipartFile file = csvFile(csv);

        when(retailerRepository.existsByTenantIdAndRetailerCode(eq(1L), anyString())).thenReturn(false);

        UploadResult result = uploadService.upload(file);

        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.failedRecords()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorMessage()).contains("mobile");
    }

    @Test
    void emptyFile_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile("file", "retailers.csv",
                "text/csv", new byte[0]);

        assertThatThrownBy(() -> uploadService.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void unsupportedFileType_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile("file", "retailers.txt",
                "text/plain", "data".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> uploadService.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV and Excel");
    }

    @Test
    void partialFailure_successAndFailedCountedCorrectly() {
        String csv = "retailer_code,retailer_name,mobile\n" +
                     "RET001,Good Retailer,9876543210\n" +
                     "RET002,Bad Mobile,NOTANUMBER\n" +
                     "RET003,Another Good,9876543212\n";
        MockMultipartFile file = csvFile(csv);

        when(retailerRepository.existsByTenantIdAndRetailerCode(eq(1L), anyString())).thenReturn(false);
        when(retailerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        UploadResult result = uploadService.upload(file);

        assertThat(result.totalRecords()).isEqualTo(3);
        assertThat(result.successRecords()).isEqualTo(2);
        assertThat(result.failedRecords()).isEqualTo(1);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "retailers.csv",
                "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
