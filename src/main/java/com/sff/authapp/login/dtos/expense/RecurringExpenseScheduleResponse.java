package com.sff.authapp.login.dtos.expense;

import java.math.BigDecimal;
import java.util.List;

public record RecurringExpenseScheduleResponse(
        BigDecimal totalMonthlyCommitment,
        long activeRecurringCount,
        long loggedThisMonthCount,
        long pendingThisMonthCount,
        List<RecurringExpenseItemDto> recurringExpenses
) {
}
