package com.sff.authapp.login.dtos.payment;

import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(

        Long paymentId,

        Long orderId,

        BigDecimal amount,

        PaymentMethod paymentMethod,

        PaymentType paymentType,

        LocalDateTime paidAt,

        String note,

        Long receivedByUserId,

        LocalDateTime createdAt
) {
}