package com.sff.authapp.login.dtos.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DeliveredUnpaidOrderResponse(
        Long orderId,
        LocalDate orderDate,
        Long customerId,
        String customerName,
        String customerPhone,
        String customerCode,
        BigDecimal grandTotal,
        BigDecimal totalPaid,
        BigDecimal balanceDue,
        int totalGarments,
        int deliveredGarments,
        LocalDateTime deliveryDate
) {
}
