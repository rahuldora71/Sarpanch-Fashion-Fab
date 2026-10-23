package com.sff.authapp.login.dtos.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerProfilePayment(

        Long paymentId,

        Long orderId,

        BigDecimal amount,

        String paymentMethod,

        String paymentType,

        LocalDateTime paidAt

) {
}