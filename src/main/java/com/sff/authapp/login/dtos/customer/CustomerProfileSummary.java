package com.sff.authapp.login.dtos.customer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerProfileSummary(

        long totalOrders,

        BigDecimal totalSpent,

        BigDecimal currentBalance,

        LocalDate lastOrderDate

) {
}