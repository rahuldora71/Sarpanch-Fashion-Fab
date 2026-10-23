package com.sff.authapp.login.dtos.expense;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseItemDto(
        Long id,
        String expenseNumber,
        String category,
        String description,
        BigDecimal amount,
        String recurringFrequency,
        Integer recurringDayOfMonth,
        String paidTo,
        String paymentMethod,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate lastLoggedDate,
        boolean loggedForCurrentMonth,
        String notes
) {
}
