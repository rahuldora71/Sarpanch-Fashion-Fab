package com.sff.authapp.login.dtos.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalAmount,
        BigDecimal recurringTotal,
        BigDecimal oneTimeTotal,
        long totalCount,
        long recurringCount,
        long oneTimeCount,
        BigDecimal averageExpense,
        List<ExpenseCategorySummaryDto> categoryBreakdown,
        List<ExpenseResponse> expenses
) {
}
