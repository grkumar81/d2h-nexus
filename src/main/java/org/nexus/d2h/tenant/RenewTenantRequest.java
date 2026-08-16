package org.nexus.d2h.tenant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record RenewTenantRequest(
        @NotNull @Future
        LocalDate subscriptionExpiry,

        @Positive
        Integer gracePeriodDays
) {}
