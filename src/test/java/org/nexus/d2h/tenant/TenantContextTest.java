package org.nexus.d2h.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_returnsSameTenantCode() {
        TenantContext.setCurrentTenant("DIST001");
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("DIST001");
    }

    @Test
    void clear_removesValue() {
        TenantContext.setCurrentTenant("DIST001");
        TenantContext.clear();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void initialState_isNull() {
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void threadIsolation_differentThreadsHaveDifferentValues() throws InterruptedException {
        TenantContext.setCurrentTenant("DIST001");

        AtomicReference<String> otherThreadValue = new AtomicReference<>();
        Thread other = new Thread(() -> {
            TenantContext.setCurrentTenant("DIST002");
            otherThreadValue.set(TenantContext.getCurrentTenant());
            TenantContext.clear();
        });
        other.start();
        other.join();

        // Main thread value must be unaffected
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("DIST001");
        assertThat(otherThreadValue.get()).isEqualTo("DIST002");
    }
}
