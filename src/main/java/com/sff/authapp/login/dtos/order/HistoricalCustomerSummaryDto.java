package com.sff.authapp.login.dtos.order;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistoricalCustomerSummaryDto(
        Long id,
        String customerCode,
        String name,
        String phone,
        String alternatePhone,
        String address,
        String image,
        long totalOrders,
        BigDecimal totalSpent,
        BigDecimal currentBalance,
        LocalDate lastOrderDate
) {
}
