package com.sff.authapp.login.dtos.order;

import com.sff.authapp.login.entities.Order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderSummaryResponse(

        Long id,

        String orderNumber,

        Long customerId,

        String customerCode,

        String customerName,

        String customerPhone,

        int garmentCount,

        BigDecimal total,

        BigDecimal paid,

        BigDecimal due,

        LocalDate deliveryDate,

        LocalDateTime createdAt

//        Order.Status status
) {
}