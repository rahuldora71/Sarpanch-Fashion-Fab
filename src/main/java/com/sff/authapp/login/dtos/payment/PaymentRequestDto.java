package com.sff.authapp.login.dtos.payment;

import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRequestDto(

        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Payment amount must be greater than 0"
        )
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Payment type is required")
        PaymentType paymentType,

        LocalDateTime paidAt,

        String note
) {
}