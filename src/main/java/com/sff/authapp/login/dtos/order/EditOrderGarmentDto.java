package com.sff.authapp.login.dtos.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EditOrderGarmentDto(

        // If provided, updates existing garment. If null, creates new garment.
        Long garmentId,

        @NotNull(message = "Service ID is required")
        Long serviceId,

        BigDecimal price,

        @Size(max = 50)
        String sampleNumber,

        @NotNull(message = "Delivery / Due date is mandatory")
        LocalDate dueDate,

        LocalDate tryDate,

        @Size(max = 5000)
        String note,

        Boolean customerFabric,

        @Valid
        @NotNull(message = "Measurement is required")
        CompleteOrderMeasurementDto measurement,

        @Valid
        List<CompleteOrderAddOnDto> addOns
) {}
