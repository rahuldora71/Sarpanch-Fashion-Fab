package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "expenses",
        indexes = {
                @Index(
                        name = "idx_expense_category",
                        columnList = "category"
                ),
                @Index(
                        name = "idx_expense_date",
                        columnList = "expense_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "expense_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String expenseNumber;


    @Column(
            nullable = false,
            length = 100
    )
    private String category;


    @Column(
            nullable = false,
            length = 500
    )
    private String description;


    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;


    @Column(
            name = "expense_date",
            nullable = false
    )
    private LocalDateTime expenseDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "added_by",
            nullable = false
    )
    private User addedBy;


    @PrePersist
    protected void onCreate() {

        if (expenseDate == null) {
            expenseDate = LocalDateTime.now();
        }
    }
}