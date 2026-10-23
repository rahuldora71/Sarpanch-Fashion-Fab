package com.sff.authapp.login.dtos.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteOrderAddOnDto(

        @NotNull(message = "Add-on ID is required")
        Long addOnId,

        @NotNull(message = "Add-on quantity is required")
        @Min(value = 1, message = "Add-on quantity must be at least 1")
        Integer quantity,

        @Size(max = 5000)
        String note

) {}