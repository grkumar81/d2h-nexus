package org.nexus.d2h.asset;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TagAssetRequest(
        @NotNull Long retailerId,
        LocalDate taggingDate,
        String remarks
) {}
