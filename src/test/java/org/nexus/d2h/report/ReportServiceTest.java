package org.nexus.d2h.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.finance.FinancialTransactionRepository;
import org.nexus.d2h.recharge.RechargeTransactionRepository;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock FinancialTransactionRepository financeRepo;
    @Mock RechargeTransactionRepository rechargeRepo;
    @Mock RetailerRepository retailerRepo;
    @Mock TenantRepository tenantRepository;
    @InjectMocks ReportService reportService;

    private Tenant tenant;
    private Retailer retailer;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantCode("T1");
        setId(tenant, 1L);

        retailer = new Retailer();
        retailer.setTenant(tenant);
        retailer.setRetailerCode("RET001");
        retailer.setRetailerName("Test Retailer");
        retailer.setMobile("9876543210");
        setId(retailer, 5L);

        TenantContext.setCurrentTenant("T1");
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void allRetailerReport_returnsAggregatedRows() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{5L, "RET001", "Test Retailer",
                BigDecimal.valueOf(100000), BigDecimal.valueOf(70000), BigDecimal.valueOf(5000)});
        when(financeRepo.allRetailerReport(eq(1L), any(), any())).thenReturn((List) rows);

        List<RetailerReportDto> result = reportService.allRetailerReport(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).retailerCode()).isEqualTo("RET001");
        assertThat(result.get(0).boxSales()).isEqualByComparingTo("100000");
        assertThat(result.get(0).received()).isEqualByComparingTo("70000");
        assertThat(result.get(0).outstanding()).isEqualByComparingTo("30000");
        assertThat(result.get(0).recharge()).isEqualByComparingTo("5000");
    }

    @Test
    @SuppressWarnings("unchecked")
    void allRetailerReport_emptyTenant_returnsEmptyList() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(financeRepo.allRetailerReport(eq(1L), any(), any())).thenReturn((List) new ArrayList<>());

        List<RetailerReportDto> result = reportService.allRetailerReport(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void retailerReport_noDateRange_usesRetailerAggregates() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepo.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(financeRepo.sumBoxSalesByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(50000));
        when(financeRepo.sumPaymentsReceivedByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(30000));
        when(financeRepo.sumRechargeByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(10000));

        RetailerReportDto dto = reportService.retailerReport(5L, null, null);

        assertThat(dto.retailerCode()).isEqualTo("RET001");
        assertThat(dto.outstanding()).isEqualByComparingTo("20000");
    }

    @Test
    void retailerReport_withDateRange_usesDateRangeAggregates() {
        LocalDate from = LocalDate.of(2025, 4, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepo.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(financeRepo.sumBoxSalesByTenantAndDateRange(1L, from, to)).thenReturn(BigDecimal.valueOf(80000));
        when(financeRepo.sumPaymentsReceivedByTenantAndDateRange(1L, from, to)).thenReturn(BigDecimal.valueOf(60000));
        when(financeRepo.sumRechargeByTenantAndDateRange(1L, from, to)).thenReturn(BigDecimal.valueOf(15000));

        RetailerReportDto dto = reportService.retailerReport(5L, from, to);

        assertThat(dto.outstanding()).isEqualByComparingTo("20000");
    }

    @Test
    void retailerReport_notFound_throwsResourceNotFoundException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepo.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.retailerReport(99L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void periodReport_returnsCorrectSummary() {
        LocalDate from = LocalDate.of(2025, 4, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(financeRepo.sumBoxSalesByTenantAndDateRange(1L, from, to)).thenReturn(BigDecimal.valueOf(200000));
        when(financeRepo.sumPaymentsReceivedByTenantAndDateRange(1L, from, to)).thenReturn(BigDecimal.valueOf(150000));
        when(financeRepo.sumRechargeByTenantAndDateRange(1L, from, to)).thenReturn(BigDecimal.valueOf(30000));
        when(financeRepo.countPostedByTenantAndDateRange(1L, from, to)).thenReturn(100L);

        PeriodReportDto dto = reportService.periodReport(from, to);

        assertThat(dto.boxSales()).isEqualByComparingTo("200000");
        assertThat(dto.outstanding()).isEqualByComparingTo("50000");
        assertThat(dto.transactionCount()).isEqualTo(100L);
    }

    @Test
    void periodReport_nullDates_usesDefaults() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(financeRepo.sumBoxSalesByTenantAndDateRange(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(financeRepo.sumPaymentsReceivedByTenantAndDateRange(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(financeRepo.sumRechargeByTenantAndDateRange(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(financeRepo.countPostedByTenantAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        PeriodReportDto dto = reportService.periodReport(null, null);

        assertThat(dto.outstanding()).isEqualByComparingTo("0");
    }

    @Test
    void missingTenantContext_throwsBusinessException() {
        TenantContext.clear();

        assertThatThrownBy(() -> reportService.allRetailerReport(null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "TENANT_CONTEXT_MISSING");
    }

    @Test
    void tenantIsolation_retailerNotInTenant_throwsResourceNotFoundException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepo.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.retailerReport(99L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
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
