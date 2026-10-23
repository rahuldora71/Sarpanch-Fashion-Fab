package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.expense.*;
import com.sff.authapp.login.entities.Expense;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.image.service.ImageService;
import com.sff.authapp.login.repositories.ExpenseRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.services.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    private static final DateTimeFormatter DATE_PREFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    @Transactional
    public ExpenseResponse createExpense(ExpenseCreateRequest request) {
        User currentUser = getCurrentUser();
        String expenseNumber = generateNextExpenseNumber();

        LocalDate reqDate = request.expenseDate() != null
                ? request.expenseDate()
                : LocalDate.now();

        LocalDateTime expenseDate = reqDate.equals(LocalDate.now())
                ? LocalDateTime.now()
                : reqDate.atTime(12, 0, 0);

        boolean isRecurring = Boolean.TRUE.equals(request.isRecurring());
        String frequency = isRecurring
                ? (request.recurringFrequency() != null && !request.recurringFrequency().isBlank() ? request.recurringFrequency().trim().toUpperCase() : "MONTHLY")
                : null;

        Expense expense = Expense.builder()
                .expenseNumber(expenseNumber)
                .category(request.category().trim().toUpperCase())
                .description(request.description().trim())
                .amount(request.amount())
                .expenseDate(expenseDate)
                .isRecurring(isRecurring)
                .recurringFrequency(frequency)
                .recurringDayOfMonth(request.recurringDayOfMonth())
                .paymentMethod(request.paymentMethod() != null && !request.paymentMethod().isBlank() ? request.paymentMethod().trim().toUpperCase() : "CASH")
                .paidTo(request.paidTo() != null ? request.paidTo().trim() : null)
                .receiptUrl(request.receiptUrl() != null && !request.receiptUrl().isBlank() ? request.receiptUrl().trim() : null)
                .receiptPublicId(request.receiptPublicId() != null && !request.receiptPublicId().isBlank() ? request.receiptPublicId().trim() : null)
                .notes(request.notes() != null ? request.notes().trim() : null)
                .addedBy(currentUser)
                .build();

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseUpdateRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (request.category() != null && !request.category().isBlank()) {
            expense.setCategory(request.category().trim().toUpperCase());
        }

        if (request.description() != null && !request.description().isBlank()) {
            expense.setDescription(request.description().trim());
        }

        if (request.amount() != null) {
            expense.setAmount(request.amount());
        }

        if (request.expenseDate() != null) {
            LocalDate reqDate = request.expenseDate();
            expense.setExpenseDate(reqDate.atTime(12, 0, 0));
        }

        if (request.isRecurring() != null) {
            expense.setRecurring(request.isRecurring());
            if (Boolean.TRUE.equals(request.isRecurring())) {
                if (request.recurringFrequency() != null && !request.recurringFrequency().isBlank()) {
                    expense.setRecurringFrequency(request.recurringFrequency().trim().toUpperCase());
                } else if (expense.getRecurringFrequency() == null) {
                    expense.setRecurringFrequency("MONTHLY");
                }
            } else {
                expense.setRecurringFrequency(null);
            }
        }

        if (request.recurringDayOfMonth() != null) {
            expense.setRecurringDayOfMonth(request.recurringDayOfMonth());
        }

        if (request.paymentMethod() != null && !request.paymentMethod().isBlank()) {
            expense.setPaymentMethod(request.paymentMethod().trim().toUpperCase());
        }

        if (request.paidTo() != null) {
            expense.setPaidTo(request.paidTo().trim());
        }

        if (request.receiptUrl() != null) {
            String oldPublicId = expense.getReceiptPublicId();
            expense.setReceiptUrl(request.receiptUrl().isBlank() ? null : request.receiptUrl().trim());
            expense.setReceiptPublicId(request.receiptPublicId() != null && !request.receiptPublicId().isBlank() ? request.receiptPublicId().trim() : null);

            if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(expense.getReceiptPublicId())) {
                try {
                    imageService.deleteImage(oldPublicId);
                } catch (Exception ignored) {
                }
            }
        }

        if (request.notes() != null) {
            expense.setNotes(request.notes().trim());
        }

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return mapToResponse(expense);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        String publicId = expense.getReceiptPublicId();
        expenseRepository.delete(expense);

        if (publicId != null && !publicId.isBlank()) {
            try {
                imageService.deleteImage(publicId);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpensesByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            String category,
            Boolean recurring
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(LocalTime.MAX);

        List<Expense> expenses;
        boolean hasCategoryFilter = category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL");

        if (hasCategoryFilter) {
            expenses = expenseRepository.findByCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(
                    category.trim().toUpperCase(),
                    startDt,
                    endDt
            );
        } else {
            expenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(
                    startDt,
                    endDt
            );
        }

        if (recurring != null) {
            expenses = expenses.stream()
                    .filter(e -> e.isRecurring() == recurring)
                    .toList();
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal recurringTotal = BigDecimal.ZERO;
        BigDecimal oneTimeTotal = BigDecimal.ZERO;
        long recurringCount = 0;
        long oneTimeCount = 0;

        Map<String, BigDecimal> categorySums = new HashMap<>();
        Map<String, Long> categoryCounts = new HashMap<>();

        for (Expense exp : expenses) {
            BigDecimal amt = exp.getAmount() != null ? exp.getAmount() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(amt);

            if (exp.isRecurring()) {
                recurringTotal = recurringTotal.add(amt);
                recurringCount++;
            } else {
                oneTimeTotal = oneTimeTotal.add(amt);
                oneTimeCount++;
            }

            String cat = exp.getCategory() != null ? exp.getCategory() : "MISCELLANEOUS";
            categorySums.merge(cat, amt, BigDecimal::add);
            categoryCounts.merge(cat, 1L, Long::sum);
        }

        long totalCount = expenses.size();
        BigDecimal averageExpense = totalCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Build category breakdown
        List<ExpenseCategorySummaryDto> breakdown = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categorySums.entrySet()) {
            double pct = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().divide(totalAmount, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                    : 0.0;
            breakdown.add(new ExpenseCategorySummaryDto(
                    entry.getKey(),
                    entry.getValue(),
                    categoryCounts.getOrDefault(entry.getKey(), 0L),
                    Math.round(pct * 10.0) / 10.0
            ));
        }
        breakdown.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        List<ExpenseResponse> responseList = expenses.stream()
                .map(this::mapToResponse)
                .toList();

        return new ExpenseSummaryResponse(
                start,
                end,
                totalAmount,
                recurringTotal,
                oneTimeTotal,
                totalCount,
                recurringCount,
                oneTimeCount,
                averageExpense,
                breakdown,
                responseList
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringExpenseScheduleResponse getRecurringExpensesSchedule() {
        List<Expense> allRecurring = expenseRepository.findByIsRecurringTrueOrderByRecurringDayOfMonthAsc();

        // Group by normalized description and category to identify recurring commitments
        Map<String, Expense> templatesMap = new LinkedHashMap<>();
        for (Expense exp : allRecurring) {
            String key = exp.getCategory() + "::" + exp.getDescription().toLowerCase().trim();
            // Keep the latest record as the representative template
            if (!templatesMap.containsKey(key) || exp.getExpenseDate().isAfter(templatesMap.get(key).getExpenseDate())) {
                templatesMap.put(key, exp);
            }
        }

        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);

        List<Expense> thisMonthExpenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(monthStart, monthEnd);

        List<RecurringExpenseItemDto> items = new ArrayList<>();
        BigDecimal totalCommitment = BigDecimal.ZERO;
        long loggedThisMonthCount = 0;

        for (Expense template : templatesMap.values()) {
            BigDecimal amt = template.getAmount() != null ? template.getAmount() : BigDecimal.ZERO;
            totalCommitment = totalCommitment.add(amt);

            // Check if logged in current month
            boolean loggedThisMonth = thisMonthExpenses.stream().anyMatch(e ->
                    e.getCategory().equalsIgnoreCase(template.getCategory()) &&
                            e.getDescription().trim().equalsIgnoreCase(template.getDescription().trim())
            );

            if (loggedThisMonth) {
                loggedThisMonthCount++;
            }

            items.add(new RecurringExpenseItemDto(
                    template.getId(),
                    template.getExpenseNumber(),
                    template.getCategory(),
                    template.getDescription(),
                    template.getAmount(),
                    template.getRecurringFrequency() != null ? template.getRecurringFrequency() : "MONTHLY",
                    template.getRecurringDayOfMonth(),
                    template.getPaidTo(),
                    template.getPaymentMethod(),
                    template.getExpenseDate() != null ? template.getExpenseDate().toLocalDate() : null,
                    loggedThisMonth,
                    template.getNotes()
            ));
        }

        long activeCount = items.size();
        long pendingThisMonthCount = Math.max(0, activeCount - loggedThisMonthCount);

        return new RecurringExpenseScheduleResponse(
                totalCommitment,
                activeCount,
                loggedThisMonthCount,
                pendingThisMonthCount,
                items
        );
    }

    @Override
    @Transactional
    public ExpenseResponse logRecurringExpenseInstance(
            Long recurringExpenseId,
            LocalDate targetDate,
            BigDecimal customAmount,
            String paymentMethod,
            String notes
    ) {
        Expense template = expenseRepository.findById(recurringExpenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense template not found with id: " + recurringExpenseId));

        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        BigDecimal amount = customAmount != null && customAmount.compareTo(BigDecimal.ZERO) > 0
                ? customAmount
                : template.getAmount();

        Expense newEntry = Expense.builder()
                .expenseNumber(generateNextExpenseNumber())
                .category(template.getCategory())
                .description(template.getDescription())
                .amount(amount)
                .expenseDate(date.atTime(LocalTime.now()))
                .isRecurring(true)
                .recurringFrequency(template.getRecurringFrequency())
                .recurringDayOfMonth(template.getRecurringDayOfMonth())
                .paymentMethod(paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod.trim().toUpperCase() : template.getPaymentMethod())
                .paidTo(template.getPaidTo())
                .receiptUrl(null)
                .receiptPublicId(null)
                .notes(notes != null && !notes.isBlank() ? notes.trim() : "Recurring monthly payment for " + date.getMonth().name())
                .addedBy(getCurrentUser())
                .build();

        Expense saved = expenseRepository.save(newEntry);
        return mapToResponse(saved);
    }

    private synchronized String generateNextExpenseNumber() {
        String yearMonth = LocalDate.now().format(DATE_PREFIX_FORMAT);
        Optional<Expense> top = expenseRepository.findTopByOrderByIdDesc();
        long nextSeq = top.map(e -> e.getId() + 1).orElse(1L);
        return String.format("EXP-%s-%04d", yearMonth, nextSeq);
    }

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                Optional<User> userOpt = userRepository.findByEmail(auth.getName());
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }
        } catch (Exception ignored) {
        }
        // Fallback to first user in repository
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private ExpenseResponse mapToResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getExpenseNumber(),
                e.getCategory(),
                e.getDescription(),
                e.getAmount(),
                e.getExpenseDate() != null ? e.getExpenseDate().toLocalDate() : null,
                e.isRecurring(),
                e.getRecurringFrequency(),
                e.getRecurringDayOfMonth(),
                e.getPaymentMethod(),
                e.getPaidTo(),
                e.getReceiptUrl(),
                e.getReceiptPublicId(),
                e.getNotes(),
                e.getAddedBy() != null ? e.getAddedBy().getName() : "System",
                e.getAddedBy() != null ? e.getAddedBy().getId() : null,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
