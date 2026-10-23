package com.sff.authapp.login.dtos.alteration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record AlterationBatchRequestDto(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Customer ID is required")
        Long customerId,

        LocalDate deliveryDate,

        String overallNote,

        boolean updateCustomerDefaultMeasurements,

        @NotEmpty(message = "At least one garment alteration is required")
        List<GarmentAlterationItemDto> garmentAlterations
) {
}
