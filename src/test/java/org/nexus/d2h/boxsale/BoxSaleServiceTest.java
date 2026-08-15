package org.nexus.d2h.boxsale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.asset.AssetService;
import org.nexus.d2h.asset.AssetStatus;
import org.nexus.d2h.asset.StbAsset;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoxSaleServiceTest {

    @Mock SaleRepository saleRepository;
    @Mock AssetService assetService;
    @Mock TenantRepository tenantRepository;
    @Mock RetailerRepository retailerRepository;
    @InjectMocks BoxSaleService boxSaleService;

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
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of()));
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_success_singleItem() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        StbAsset asset = assetWithId(10L);
        when(assetService.markSold(eq(10L), eq(1L), any(), any())).thenReturn(asset);
        when(saleRepository.save(any())).thenAnswer(inv -> {
            StbSale s = inv.getArgument(0);
            setId(s, 100L);
            return s;
        });

        var request = new CreateSaleRequest(5L, LocalDate.now(),
                List.of(new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500))),
                "REF001", null);

        SaleDto dto = boxSaleService.create(request);

        assertThat(dto.totalAmount()).isEqualByComparingTo("1500");
        assertThat(dto.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(dto.items()).hasSize(1);
    }

    @Test
    void create_multipleItems_totalSummed() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(assetService.markSold(eq(10L), eq(1L), any(), any())).thenReturn(assetWithId(10L));
        when(assetService.markSold(eq(11L), eq(1L), any(), any())).thenReturn(assetWithId(11L));
        when(saleRepository.save(any())).thenAnswer(inv -> { setId(inv.getArgument(0), 100L); return inv.getArgument(0); });

        var request = new CreateSaleRequest(5L, LocalDate.now(), List.of(
                new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500)),
                new CreateSaleRequest.SaleItemRequest(11L, BigDecimal.valueOf(2000))
        ), null, null);

        SaleDto dto = boxSaleService.create(request);

        assertThat(dto.totalAmount()).isEqualByComparingTo("3500");
        assertThat(dto.items()).hasSize(2);
    }

    @Test
    void create_duplicateAssetInRequest_throwsBusinessException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));

        var request = new CreateSaleRequest(5L, LocalDate.now(), List.of(
                new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500)),
                new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500))
        ), null, null);

        assertThatThrownBy(() -> boxSaleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_ASSET_IN_SALE");
    }

    @Test
    void create_unavailableAsset_throwsBusinessException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(assetService.markSold(eq(10L), eq(1L), any(), any()))
                .thenThrow(new BusinessException("ASSET_NOT_SALEABLE", "Asset is not available for sale"));

        var request = new CreateSaleRequest(5L, LocalDate.now(),
                List.of(new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500))),
                null, null);

        assertThatThrownBy(() -> boxSaleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_NOT_SALEABLE");
    }

    @Test
    void create_retailerNotFound_throwsResourceNotFoundException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        var request = new CreateSaleRequest(99L, LocalDate.now(),
                List.of(new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500))),
                null, null);

        assertThatThrownBy(() -> boxSaleService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(saleRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boxSaleService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_byRetailer_returnsPaginatedResults() {
        StbSale sale = saleWithId(100L);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(saleRepository.findByTenantIdAndRetailerId(eq(1L), eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = boxSaleService.list(5L, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void create_missingTenantContext_throwsBusinessException() {
        TenantContext.clear();

        var request = new CreateSaleRequest(5L, LocalDate.now(),
                List.of(new CreateSaleRequest.SaleItemRequest(10L, BigDecimal.valueOf(1500))),
                null, null);

        assertThatThrownBy(() -> boxSaleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private StbAsset assetWithId(Long id) {
        StbAsset a = new StbAsset();
        a.setTenant(tenant);
        a.setSerialNumber("SN00" + id);
        a.setStatus(AssetStatus.AVAILABLE);
        setId(a, id);
        return a;
    }

    private StbSale saleWithId(Long id) {
        StbSale s = new StbSale();
        s.setTenant(tenant);
        s.setRetailer(retailer);
        s.setTransactionDate(LocalDate.now());
        s.setTotalAmount(BigDecimal.valueOf(1500));
        s.setPaymentStatus(PaymentStatus.PENDING);
        setId(s, id);
        return s;
    }

    private void setId(Object entity, Long id) {
        try {
            Class<?> cls = entity.getClass();
            java.lang.reflect.Field field = null;
            while (cls != null) {
                try { field = cls.getDeclaredField("id"); break; } catch (NoSuchFieldException e) { cls = cls.getSuperclass(); }
            }
            if (field == null) throw new NoSuchFieldException("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
