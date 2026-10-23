package com.sff.authapp.login.dtos.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderRequestDto(

        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Order date is required")
        LocalDate orderDate,

        @Size(
                max = 50,
                message = "Sample book page number cannot exceed 50 characters"
        )
        String sampleBookPageNumber,
        BigDecimal discount
) {
}