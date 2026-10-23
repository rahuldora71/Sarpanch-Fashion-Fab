package com.sff.authapp.login.dtos.expense;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCreateRequest(
        @NotBlank(message = "Expense category is required")
        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
        LocalDate expenseDate,

        Boolean isRecurring,

        String recurringFrequency,

        Integer recurringDayOfMonth,

        String paymentMethod,

        @Size(max = 200, message = "Paid to cannot exceed 200 characters")
        String paidTo,

        @Size(max = 1000, message = "Receipt URL cannot exceed 1000 characters")
        String receiptUrl,

        @Size(max = 255, message = "Receipt public ID cannot exceed 255 characters")
        String receiptPublicId,

        String notes
) {
}
