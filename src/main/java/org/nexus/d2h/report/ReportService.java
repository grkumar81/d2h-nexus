package org.nexus.d2h.report;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.finance.FinancialTransactionRepository;
import org.nexus.d2h.recharge.RechargeTransactionRepository;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReportService {

    private final FinancialTransactionRepository financeRepo;
    private final RechargeTransactionRepository rechargeRepo;
    private final RetailerRepository retailerRepo;
    private final TenantRepository tenantRepository;

    public ReportService(FinancialTransactionRepository financeRepo,
                         RechargeTransactionRepository rechargeRepo,
                         RetailerRepository retailerRepo,
                         TenantRepository tenantRepository) {
        this.financeRepo = financeRepo;
        this.rechargeRepo = rechargeRepo;
        this.retailerRepo = retailerRepo;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<RetailerReportDto> allRetailerReport(LocalDate dateFrom, LocalDate dateTo) {
        Long tenantId = resolveTenant().getId();
        List<Object[]> rows = financeRepo.allRetailerReport(tenantId, dateFrom, dateTo);
        List<RetailerReportDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            BigDecimal boxSales = toBigDecimal(row[3]);
            BigDecimal received = toBigDecimal(row[4]);
            BigDecimal recharge = toBigDecimal(row[5]);
            result.add(new RetailerReportDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    boxSales,
                    received,
                    boxSales.subtract(received),
                    recharge
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public RetailerReportDto retailerReport(Long retailerId, LocalDate dateFrom, LocalDate dateTo) {
        Tenant tenant = resolveTenant();
        Retailer retailer = retailerRepo.findByIdAndTenantId(retailerId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));

        BigDecimal boxSales = dateFrom != null
                ? financeRepo.sumBoxSalesByTenantAndDateRange(tenant.getId(), dateFrom, dateTo)
                : financeRepo.sumBoxSalesByRetailer(tenant.getId(), retailerId);
        BigDecimal received = dateFrom != null
                ? financeRepo.sumPaymentsReceivedByTenantAndDateRange(tenant.getId(), dateFrom, dateTo)
                : financeRepo.sumPaymentsReceivedByRetailer(tenant.getId(), retailerId);
        BigDecimal recharge = dateFrom != null
                ? financeRepo.sumRechargeByTenantAndDateRange(tenant.getId(), dateFrom, dateTo)
                : financeRepo.sumRechargeByRetailer(tenant.getId(), retailerId);

        return new RetailerReportDto(
                retailer.getId(), retailer.getRetailerCode(), retailer.getRetailerName(),
                boxSales, received, boxSales.subtract(received), recharge
        );
    }

    @Transactional(readOnly = true)
    public PeriodReportDto periodReport(LocalDate dateFrom, LocalDate dateTo) {
        Long tenantId = resolveTenant().getId();
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.of(2000, 1, 1);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();

        BigDecimal boxSales = financeRepo.sumBoxSalesByTenantAndDateRange(tenantId, from, to);
        BigDecimal received = financeRepo.sumPaymentsReceivedByTenantAndDateRange(tenantId, from, to);
        BigDecimal recharge = financeRepo.sumRechargeByTenantAndDateRange(tenantId, from, to);
        long count = financeRepo.countPostedByTenantAndDateRange(tenantId, from, to);

        return new PeriodReportDto(dateFrom, dateTo, boxSales, received,
                boxSales.subtract(received), recharge, count);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private Tenant resolveTenant() {
        String tenantCode = TenantContext.getCurrentTenant();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
        return tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantCode));
    }
}
