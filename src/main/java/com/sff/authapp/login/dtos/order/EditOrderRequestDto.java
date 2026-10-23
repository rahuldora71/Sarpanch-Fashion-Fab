package com.sff.authapp.login.dtos.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for editing an existing order with differential updates.
 * Pre-existing garments include their garmentId. New garments have null garmentId.
 */
public record EditOrderRequestDto(

        @NotNull(message = "Order date is required")
        LocalDate orderDate,

        @Size(max = 50, message = "Sample book page number cannot exceed 50 characters")
        String sampleBookPageNumber,

        BigDecimal discount,

        @NotEmpty(message = "At least one garment is required")
        @Valid
        List<EditOrderGarmentDto> garments,

        @Valid
        List<CompleteOrderChargeDto> additionalCharges,

        @Valid
        CompleteOrderPaymentDto payment
) {}
