package com.sff.authapp.login.dtos.customer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerSummaryResponse(

        Long id,

        String customerCode,

        String name,

        String phone,

        String email,


        String image,
        long totalOrders,

        BigDecimal totalSpent,

        BigDecimal balance,

        LocalDate lastOrderDate



) {
}