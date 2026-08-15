package org.nexus.d2h.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAssetRequest(
        @NotBlank @Size(max = 100) String serialNumber,
        @Size(max = 100) String boxNumber,
        @Size(max = 100) String model,
        @Size(max = 100) String manufacturer,
        @Size(max = 100) String batch,
        LocalDate purchaseDate,
        BigDecimal purchaseCost
) {}
