package com.sff.authapp.login.dtos.expense;

import java.math.BigDecimal;

public record ExpenseCategorySummaryDto(
        String category,
        BigDecimal totalAmount,
        long count,
        double percentage
) {
}
