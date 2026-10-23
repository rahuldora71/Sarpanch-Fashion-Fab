package com.sff.authapp.login.dtos.expense;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        String expenseNumber,
        String category,
        String description,
        BigDecimal amount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate expenseDate,
        boolean isRecurring,
        String recurringFrequency,
        Integer recurringDayOfMonth,
        String paymentMethod,
        String paidTo,
        String receiptUrl,
        String receiptPublicId,
        String notes,
        String addedByName,
        Long addedById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
