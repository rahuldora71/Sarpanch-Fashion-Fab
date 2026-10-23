package com.sff.authapp.login.dtos.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompleteOrderGarmentDto(

        @NotNull(message = "Service ID is required")
        Long serviceId,

        /*
         * Optional custom price.
         *
         * If provided, backend uses it.
         * If null, backend uses TailoringService.price.
         */
        @jakarta.validation.constraints.DecimalMin(
                value = "0.00",
                message = "Price cannot be negative"
        )
        BigDecimal price,

        @Size(max = 50)
        String sampleNumber,

        LocalDate dueDate,

        @Size(max = 5000)
        String note,

        Boolean customerFabric,

        @Valid
        @NotNull(message = "Measurement is required")
        CompleteOrderMeasurementDto measurement,

        @Valid
        List<CompleteOrderAddOnDto> addOns

) {}