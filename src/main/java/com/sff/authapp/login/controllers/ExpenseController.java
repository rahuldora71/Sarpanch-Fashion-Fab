package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.expense.*;
import com.sff.authapp.login.services.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(
        name = "Expense Management",
        description = "APIs for recording shop expenses, recurring monthly overheads, date range tracking and breakdowns"
)
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Record new expense", description = "Creates a new one-time or recurring monthly shop expense")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseCreateRequest request) {
        ExpenseResponse created = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get expenses in date range", description = "Retrieves expenses within a date range with summary metrics and category breakdowns")
    public ResponseEntity<ExpenseSummaryResponse> getExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean recurring
    ) {
        ExpenseSummaryResponse summary = expenseService.getExpensesByDateRange(startDate, endDate, category, recurring);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get expense by ID", description = "Retrieves specific expense details")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update expense", description = "Updates an existing shop expense record")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseUpdateRequest request
    ) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Delete expense", description = "Removes an expense record and deletes associated receipt file if any")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recurring")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get recurring monthly expenses schedule", description = "Lists recurring overheads and tracks whether each has been logged/paid for the current month")
    public ResponseEntity<RecurringExpenseScheduleResponse> getRecurringSchedule() {
        return ResponseEntity.ok(expenseService.getRecurringExpensesSchedule());
    }

    @PostMapping("/recurring/{id}/log")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Quick-log recurring expense instance", description = "Quickly logs a monthly recurring expense for the current or specified month")
    public ResponseEntity<ExpenseResponse> logRecurringInstance(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam(required = false) BigDecimal customAmount,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String notes
    ) {
        ExpenseResponse logged = expenseService.logRecurringExpenseInstance(
                id,
                targetDate,
                customAmount,
                paymentMethod,
                notes
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(logged);
    }
}
