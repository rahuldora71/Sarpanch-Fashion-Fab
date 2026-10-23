package com.sff.authapp.login.dtos.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompleteOrderChargeDto(

        @NotBlank(message = "Charge description is required")
        @Size(max = 255)
        String description,

        @NotNull(message = "Charge amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Charge amount must be greater than zero"
        )
        BigDecimal amount

) {}