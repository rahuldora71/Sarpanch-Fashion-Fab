package com.sff.authapp.login.dtos.order;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderUpdateRequest(

        @FutureOrPresent(
                message = "Delivery date cannot be in the past"
        )
        LocalDate deliveryDate,

        @DecimalMin(
                value = "0.00",
                message = "Discount cannot be negative"
        )
        BigDecimal discount,

        @Size(
                max = 2000,
                message = "Notes cannot exceed 2000 characters"
        )
        String notes
) {
}