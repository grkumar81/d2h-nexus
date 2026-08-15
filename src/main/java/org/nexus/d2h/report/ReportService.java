package org.nexus.d2h.report;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.finance.FinancialTransactionRepository;
import org.nexus.d2h.recharge.RechargeTransactionRepository;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.TenantContext;
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

    public ReportService(FinancialTransactionRepository financeRepo,
                         RechargeTransactionRepository rechargeRepo,
                         RetailerRepository retailerRepo) {
        this.financeRepo = financeRepo;
        this.rechargeRepo = rechargeRepo;
        this.retailerRepo = retailerRepo;
    }

    @Transactional(readOnly = true)
    public List<RetailerReportDto> allRetailerReport(LocalDate dateFrom, LocalDate dateTo) {
        ensureTenantContext();
        List<Object[]> rows = financeRepo.allRetailerReport(dateFrom, dateTo);
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
        ensureTenantContext();
        Retailer retailer = retailerRepo.findById(retailerId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));

        BigDecimal boxSales = dateFrom != null
                ? financeRepo.sumBoxSalesByRetailer(retailerId)
                : financeRepo.sumBoxSalesByRetailer(retailerId);
        BigDecimal received = dateFrom != null
                ? financeRepo.sumPaymentsReceivedByRetailer(retailerId)
                : financeRepo.sumPaymentsReceivedByRetailer(retailerId);
        BigDecimal recharge = dateFrom != null
                ? financeRepo.sumRechargeByRetailer(retailerId)
                : financeRepo.sumRechargeByRetailer(retailerId);

        return new RetailerReportDto(
                retailer.getId(), retailer.getRetailerCode(), retailer.getRetailerName(),
                boxSales, received, boxSales.subtract(received), recharge
        );
    }

    @Transactional(readOnly = true)
    public PeriodReportDto periodReport(LocalDate dateFrom, LocalDate dateTo) {
        ensureTenantContext();
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.of(2000, 1, 1);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();

        BigDecimal boxSales = financeRepo.sumBoxSalesByDateRange(from, to);
        BigDecimal received = financeRepo.sumPaymentsReceivedByDateRange(from, to);
        BigDecimal recharge = financeRepo.sumRechargeByDateRange(from, to);
        long count = financeRepo.countPostedByDateRange(from, to);

        return new PeriodReportDto(dateFrom, dateTo, boxSales, received,
                boxSales.subtract(received), recharge, count);
    }

    private void ensureTenantContext() {
        String tenantCode = TenantContext.getCurrentTenant();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
