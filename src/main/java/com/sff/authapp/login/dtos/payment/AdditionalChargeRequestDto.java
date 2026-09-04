package com.sff.authapp.login.dtos.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdditionalChargeRequestDto(

        @NotBlank(message = "Charge description is required")
        String description,

        @NotNull(message = "Charge amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Charge amount must be greater than 0"
        )
        BigDecimal amount
) {
}