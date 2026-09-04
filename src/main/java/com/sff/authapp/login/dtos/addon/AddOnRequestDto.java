package com.sff.authapp.login.dtos.addon;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AddOnRequestDto(

        @NotBlank(message = "Add-on name is required")
        @Size(
                max = 150,
                message = "Add-on name cannot exceed 150 characters"
        )
        String name,

        @NotNull(message = "Price is required")
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

        boolean beforeCutting,

        boolean active
) {
}