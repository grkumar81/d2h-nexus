package org.nexus.d2h.asset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
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
class AssetServiceTest {

    @Mock AssetRepository assetRepository;
    @Mock AssetHistoryRepository historyRepository;
    @Mock RetailerRepository retailerRepository;
    @InjectMocks AssetService assetService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of()));
    }

    @BeforeEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_success() {
        when(assetRepository.existsBySerialNumber("SN001")).thenReturn(false);
        when(assetRepository.save(any())).thenAnswer(inv -> { setId(inv.getArgument(0), 10L); return inv.getArgument(0); });
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetDto dto = assetService.create(new CreateAssetRequest("SN001", "BX001", "ModelX", "MfgA", "B1", null, null));

        assertThat(dto.serialNumber()).isEqualTo("SN001");
        assertThat(dto.status()).isEqualTo(AssetStatus.AVAILABLE);
        verify(historyRepository).save(any(StbAssetHistory.class));
    }

    @Test
    void create_duplicateSerial_throwsBusinessException() {
        when(assetRepository.existsBySerialNumber("SN001")).thenReturn(true);

        assertThatThrownBy(() -> assetService.create(new CreateAssetRequest("SN001", null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SN001");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tag_availableAsset_transitionsToAllocated() {
        StbAsset asset = assetWithId(10L, AssetStatus.AVAILABLE);
        Retailer retailer = retailerWithId(5L);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(retailerRepository.findById(5L)).thenReturn(Optional.of(retailer));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetDto dto = assetService.tag(10L, new TagAssetRequest(5L, null, null));

        assertThat(dto.status()).isEqualTo(AssetStatus.ALLOCATED);
    }

    @Test
    void tag_soldAsset_throwsBusinessException() {
        StbAsset asset = assetWithId(10L, AssetStatus.SOLD);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.tag(10L, new TagAssetRequest(5L, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_NOT_TAGGABLE");
    }

    @Test
    void transition_scrapedAsset_throwsBusinessException() {
        StbAsset asset = assetWithId(10L, AssetStatus.SCRAPPED);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.transition(10L, AssetStatus.AVAILABLE, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_ASSET_TRANSITION");
    }

    @Test
    void transition_soldToActivated_succeeds() {
        StbAsset asset = assetWithId(10L, AssetStatus.SOLD);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetDto dto = assetService.transition(10L, AssetStatus.ACTIVATED, "Activated by customer");

        assertThat(dto.status()).isEqualTo(AssetStatus.ACTIVATED);
    }

    @Test
    void markSold_unavailableAsset_throwsBusinessException() {
        StbAsset asset = assetWithId(10L, AssetStatus.DAMAGED);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.markSold(10L, null, "user"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_NOT_SALEABLE");
    }

    @Test
    void search_returnsPaginatedResults() {
        StbAsset asset = assetWithId(10L, AssetStatus.AVAILABLE);
        when(assetRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(asset)));

        var result = assetService.search(null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private StbAsset assetWithId(Long id, AssetStatus status) {
        StbAsset a = new StbAsset();
        a.setSerialNumber("SN00" + id);
        a.setStatus(status);
        setId(a, id);
        return a;
    }

    private Retailer retailerWithId(Long id) {
        Retailer r = new Retailer();
        r.setRetailerCode("RET00" + id);
        r.setRetailerName("Retailer " + id);
        r.setMobile("9876543210");
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
