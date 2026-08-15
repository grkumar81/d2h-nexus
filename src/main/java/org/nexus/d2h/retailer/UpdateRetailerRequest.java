package org.nexus.d2h.retailer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateRetailerRequest(

        @NotBlank(message = "Retailer name is required")
        @Size(max = 255, message = "Retailer name must not exceed 255 characters")
        String retailerName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Mobile must be a 10-digit number")
        String mobile,

        @Pattern(regexp = "^[0-9]{10}$", message = "Alternate mobile must be a 10-digit number")
        String alternateMobile,

        @Size(max = 255, message = "Email must not exceed 255 characters")
        @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Invalid email format")
        String email,

        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @Pattern(regexp = "^$|^[0-9]{6}$", message = "PIN code must be 6 digits")
        String pinCode,

        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                 message = "Invalid GST number format")
        String gstNumber,

        @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN number format")
        String panNumber,

        LocalDate joiningDate
) {}
