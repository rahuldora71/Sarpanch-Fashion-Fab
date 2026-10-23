package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    List<Expense> findByCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(
            String category,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Expense> findByIsRecurringTrueOrderByRecurringDayOfMonthAsc();

    List<Expense> findByIsRecurringTrueAndExpenseDateBetweenOrderByExpenseDateDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<Expense> findByExpenseNumber(String expenseNumber);

    boolean existsByExpenseNumber(String expenseNumber);

    Optional<Expense> findTopByOrderByIdDesc();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumAmountBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.isRecurring = true AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumRecurringAmountBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0), COUNT(e) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> findCategoryBreakdownBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
