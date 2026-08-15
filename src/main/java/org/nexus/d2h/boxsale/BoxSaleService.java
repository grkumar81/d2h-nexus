package org.nexus.d2h.boxsale;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.asset.AssetService;
import org.nexus.d2h.asset.StbAsset;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.finance.FinanceService;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class BoxSaleService {

    private final SaleRepository saleRepository;
    private final AssetService assetService;
    private final RetailerRepository retailerRepository;
    private final FinanceService financeService;

    public BoxSaleService(SaleRepository saleRepository,
                          AssetService assetService,
                          RetailerRepository retailerRepository,
                          FinanceService financeService) {
        this.saleRepository = saleRepository;
        this.assetService = assetService;
        this.retailerRepository = retailerRepository;
        this.financeService = financeService;
    }

    @Transactional
    public SaleDto create(CreateSaleRequest request) {
        Retailer retailer = retailerRepository.findById(request.retailerId())
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", request.retailerId()));

        Set<Long> seen = new HashSet<>();
        for (var item : request.items()) {
            if (!seen.add(item.assetId())) {
                throw new BusinessException("DUPLICATE_ASSET_IN_SALE",
                        "Asset ID " + item.assetId() + " appears more than once in this sale");
            }
        }

        String username = currentUsername();
        StbSale sale = new StbSale();
        sale.setRetailer(retailer);
        sale.setTransactionDate(request.transactionDate());
        sale.setPaymentStatus(PaymentStatus.PENDING);
        sale.setReference(request.reference());
        sale.setRemarks(request.remarks());

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.items()) {
            StbAsset asset = assetService.markSold(itemReq.assetId(), request.transactionDate(), username);
            StbSaleItem item = new StbSaleItem(sale, asset, itemReq.unitPrice());
            sale.getItems().add(item);
            total = total.add(itemReq.unitPrice());
        }
        sale.setTotalAmount(total);

        StbSale saved = saleRepository.save(sale);
        financeService.recordBoxSale(retailer, saved);
        log.info("Box sale created: id={} retailer={} items={} total={}",
                saved.getId(), retailer.getRetailerCode(), request.items().size(), total);
        return SaleDto.from(saved);
    }

    @Transactional(readOnly = true)
    public SaleDto getById(Long id) {
        return SaleDto.from(saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleDto> list(Long retailerId, Pageable pageable) {
        Page<SaleDto> page = (retailerId != null
                ? saleRepository.findByRetailerId(retailerId, pageable)
                : saleRepository.findAll(pageable))
                .map(SaleDto::from);
        return PageResponse.from(page);
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
