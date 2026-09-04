package com.sff.authapp.login.dtos.service;

import com.sff.authapp.login.entities.MeasurementField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record TailoringServiceUpdateRequestDto(

        @Size(
                max = 150,
                message = "Service name cannot exceed 150 characters"
        )
        String name,

        @Size(
                max = 100,
                message = "Category cannot exceed 100 characters"
        )
        String category,

        @DecimalMin(
                value = "0.0",
                inclusive = true,
                message = "Price cannot be negative"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Price must have at most 10 integer digits and 2 decimal places"
        )
        BigDecimal price,

        Boolean active,

        @DecimalMin(
                value = "0.0",
                inclusive = true,
                message = "Tailor commission cannot be negative"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Commission must have at most 10 integer digits and 2 decimal places"
        )
        BigDecimal tailorCommission,

        @Valid
        List<MeasurementField> measurementFields
) {
}