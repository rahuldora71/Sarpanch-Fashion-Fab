package com.sff.authapp.login.dtos.payment;

import java.math.BigDecimal;

public record PaymentSummaryResponse(

        Long orderId,

        BigDecimal subtotal,

        BigDecimal additionalCharges,

        BigDecimal grandTotal,

        BigDecimal totalPaid,

        BigDecimal balanceDue
) {
}