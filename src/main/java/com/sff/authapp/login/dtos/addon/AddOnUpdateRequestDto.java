package com.sff.authapp.login.dtos.addon;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AddOnUpdateRequestDto(

        @Size(
                max = 150,
                message = "Add-on name cannot exceed 150 characters"
        )
        String name,

        @DecimalMin(
                value = "0.0",
                message = "Price cannot be negative"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Price can have maximum 2 decimal places"
        )
        BigDecimal price,

        @DecimalMin(
                value = "0.0",
                message = "Tailor commission cannot be negative"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Tailor commission can have maximum 2 decimal places"
        )
        BigDecimal tailorCommission,

        @Size(
                max = 1000,
                message = "Image URL cannot exceed 1000 characters"
        )
        String imageUrl,

        @Size(
                max = 500,
                message = "Image public ID cannot exceed 500 characters"
        )
        String imagePublicId,

        Boolean beforeCutting,

        Boolean active
) {
}