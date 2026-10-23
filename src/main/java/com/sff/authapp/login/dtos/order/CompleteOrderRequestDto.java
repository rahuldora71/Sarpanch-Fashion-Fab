package com.sff.authapp.login.dtos.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CompleteOrderRequestDto(

        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Order date is required")
        LocalDate orderDate,

        @Size(max = 50, message = "Sample book page number cannot exceed 50 characters")
        String sampleBookPageNumber,

        @Valid
        java.math.BigDecimal discount,


        @NotEmpty(message = "At least one garment is required")
        @Valid
        List<CompleteOrderGarmentDto> garments,

        @Valid
        List<CompleteOrderChargeDto> additionalCharges,

        @Valid
        CompleteOrderPaymentDto payment

) {}