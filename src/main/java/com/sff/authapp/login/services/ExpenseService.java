package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.expense.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseCreateRequest request);

    ExpenseResponse updateExpense(Long id, ExpenseUpdateRequest request);

    ExpenseResponse getExpenseById(Long id);

    void deleteExpense(Long id);

    ExpenseSummaryResponse getExpensesByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            String category,
            Boolean recurring
    );

    RecurringExpenseScheduleResponse getRecurringExpensesSchedule();

    ExpenseResponse logRecurringExpenseInstance(
            Long recurringExpenseId,
            LocalDate targetDate,
            BigDecimal customAmount,
            String paymentMethod,
            String notes
    );
}
