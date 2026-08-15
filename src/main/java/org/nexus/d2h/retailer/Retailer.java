package org.nexus.d2h.retailer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.common.BaseEntity;
import org.nexus.d2h.tenant.Tenant;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "retailers")
public class Retailer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "retailer_code", nullable = false, length = 50)
    private String retailerCode;

    @Column(name = "retailer_name", nullable = false, length = 255)
    private String retailerName;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(name = "alternate_mobile", length = 20)
    private String alternateMobile;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetailerStatus status = RetailerStatus.ACTIVE;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
