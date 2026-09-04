package com.sff.authapp.login.dtos.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdditionalChargeResponse(

        Long id,

        Long orderId,

        String description,

        BigDecimal amount,

        LocalDateTime createdAt
) {
}