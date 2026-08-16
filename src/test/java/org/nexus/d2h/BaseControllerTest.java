package org.nexus.d2h;

import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class for all @WebMvcTest controller slice tests.
 * Provides @MockitoBean for beans that are @Component filters
 * picked up by the WebMvc context but not relevant to controller tests.
 */
public abstract class BaseControllerTest {

    /**
     * SubscriptionFilter depends on TenantRepository.
     * Must be mocked so the WebMvc context loads successfully.
     */
    @MockitoBean
    protected TenantRepository tenantRepository;
}
