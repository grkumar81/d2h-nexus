package org.nexus.d2h.retailer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetailerServiceTest {

    @Mock RetailerRepository retailerRepository;
    @Mock TenantRepository tenantRepository;
    @InjectMocks RetailerService retailerService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantCode("TENANT1");
        // Use reflection to set id since BaseEntity uses @GeneratedValue
        try {
            var idField = org.nexus.d2h.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(tenant, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TenantContext.setCurrentTenant("TENANT1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_success() {
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.existsByTenantIdAndRetailerCode(1L, "RET001")).thenReturn(false);
        when(retailerRepository.save(any())).thenAnswer(inv -> {
            Retailer r = inv.getArgument(0);
            setId(r, 10L);
            return r;
        });

        RetailerDto dto = retailerService.create(createRequest("RET001"));

        assertThat(dto.retailerCode()).isEqualTo("RET001");
        assertThat(dto.retailerName()).isEqualTo("Test Retailer");
        assertThat(dto.status()).isEqualTo(RetailerStatus.ACTIVE);
    }

    @Test
    void create_duplicateCode_throwsBusinessException() {
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.existsByTenantIdAndRetailerCode(1L, "RET001")).thenReturn(true);

        assertThatThrownBy(() -> retailerService.create(createRequest("RET001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RET001");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retailerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_success() {
        Retailer existing = retailerWithId(10L);
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(existing));
        when(retailerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetailerDto dto = retailerService.update(10L, updateRequest("Updated Name"));

        assertThat(dto.retailerName()).isEqualTo("Updated Name");
    }

    @Test
    void activate_setsStatusActive() {
        Retailer existing = retailerWithId(10L);
        existing.setStatus(RetailerStatus.INACTIVE);
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(existing));
        when(retailerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetailerDto dto = retailerService.activate(10L);

        assertThat(dto.status()).isEqualTo(RetailerStatus.ACTIVE);
    }

    @Test
    void deactivate_setsStatusInactive() {
        Retailer existing = retailerWithId(10L);
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(existing));
        when(retailerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetailerDto dto = retailerService.deactivate(10L);

        assertThat(dto.status()).isEqualTo(RetailerStatus.INACTIVE);
    }

    @Test
    void search_returnsPaginatedResults() {
        Retailer r = retailerWithId(10L);
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        var result = retailerService.search(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void tenantIsolation_cannotAccessOtherTenantRetailer() {
        // Tenant context is TENANT1 (id=1), but retailer belongs to tenant 2
        when(tenantRepository.findByTenantCode("TENANT1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retailerService.getById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_missingTenantContext_throwsBusinessException() {
        TenantContext.clear();

        assertThatThrownBy(() -> retailerService.create(createRequest("RET001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreateRetailerRequest createRequest(String code) {
        return new CreateRetailerRequest(code, "Test Retailer", "9876543210",
                null, null, null, "Mumbai", "Maharashtra", "400001", null, null, null);
    }

    private UpdateRetailerRequest updateRequest(String name) {
        return new UpdateRetailerRequest(name, "9876543210",
                null, null, null, "Mumbai", "Maharashtra", "400001", null, null, null);
    }

    private Retailer retailerWithId(Long id) {
        Retailer r = new Retailer();
        r.setTenant(tenant);
        r.setRetailerCode("RET001");
        r.setRetailerName("Test Retailer");
        r.setMobile("9876543210");
        r.setStatus(RetailerStatus.ACTIVE);
        setId(r, id);
        return r;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = org.nexus.d2h.common.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
