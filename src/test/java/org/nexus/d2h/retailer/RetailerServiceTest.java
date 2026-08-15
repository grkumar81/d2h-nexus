package org.nexus.d2h.retailer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.audit.AuditService;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
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
    @Mock AuditService auditService;
    @InjectMocks RetailerService retailerService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
    }

    @BeforeEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_success() {
        when(retailerRepository.existsByRetailerCode("RET001")).thenReturn(false);
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
        when(retailerRepository.existsByRetailerCode("RET001")).thenReturn(true);

        assertThatThrownBy(() -> retailerService.create(createRequest("RET001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RET001");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(retailerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retailerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_success() {
        Retailer existing = retailerWithId(10L);
        when(retailerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(retailerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetailerDto dto = retailerService.update(10L, updateRequest("Updated Name"));

        assertThat(dto.retailerName()).isEqualTo("Updated Name");
    }

    @Test
    void activate_setsStatusActive() {
        Retailer existing = retailerWithId(10L);
        existing.setStatus(RetailerStatus.INACTIVE);
        when(retailerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(retailerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetailerDto dto = retailerService.activate(10L);

        assertThat(dto.status()).isEqualTo(RetailerStatus.ACTIVE);
    }

    @Test
    void deactivate_setsStatusInactive() {
        Retailer existing = retailerWithId(10L);
        when(retailerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(retailerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetailerDto dto = retailerService.deactivate(10L);

        assertThat(dto.status()).isEqualTo(RetailerStatus.INACTIVE);
    }

    @Test
    void search_returnsPaginatedResults() {
        Retailer r = retailerWithId(10L);
        when(retailerRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        var result = retailerService.search(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException_isolation() {
        when(retailerRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retailerService.getById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
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
