package com.sff.authapp.login.dtos.garmentaddon;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GarmentAddOnRequestDto(

        @NotNull(message = "Garment ID is required")
        Long garmentId,

        @NotNull(message = "Add-on ID is required")
        Long addOnId,

        @NotNull(message = "Quantity is required")
        @Min(
                value = 1,
                message = "Quantity must be at least 1"
        )
        Integer quantity,

        @Size(
                max = 5000,
                message = "Note cannot exceed 5000 characters"
        )
        String note
) {
}