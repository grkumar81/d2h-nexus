package org.nexus.d2h.retailer;

import java.time.Instant;
import java.time.LocalDate;

public record RetailerDto(
        Long id,
        String retailerCode,
        String retailerName,
        String mobile,
        String alternateMobile,
        String email,
        String address,
        String city,
        String state,
        String pinCode,
        String gstNumber,
        String panNumber,
        RetailerStatus status,
        LocalDate joiningDate,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    static RetailerDto from(Retailer r) {
        return new RetailerDto(
                r.getId(),
                r.getRetailerCode(),
                r.getRetailerName(),
                r.getMobile(),
                r.getAlternateMobile(),
                r.getEmail(),
                r.getAddress(),
                r.getCity(),
                r.getState(),
                r.getPinCode(),
                r.getGstNumber(),
                r.getPanNumber(),
                r.getStatus(),
                r.getJoiningDate(),
                r.getCreatedBy(),
                r.getUpdatedBy(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
