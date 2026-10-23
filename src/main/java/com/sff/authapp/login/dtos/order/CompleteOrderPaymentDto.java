package com.sff.authapp.login.dtos.order;

import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompleteOrderPaymentDto(

        @NotNull(message = "Payment amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Payment amount must be greater than zero"
        )
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Payment type is required")
        PaymentType paymentType,

        @Size(max = 5000)
        String note,

        @NotNull(message = "Received by user ID is required")
        Long receivedById

) {}