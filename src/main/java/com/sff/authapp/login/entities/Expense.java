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
                ),
                @Index(
                        name = "idx_expense_recurring",
                        columnList = "is_recurring"
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
            length = 50
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

    /**
     * Flag indicating if this expense occurs every month/periodically.
     */
    @Column(
            name = "is_recurring",
            nullable = false
    )
    @Builder.Default
    private boolean isRecurring = false;

    /**
     * Frequency of recurring expense (e.g. MONTHLY, QUARTERLY, YEARLY, WEEKLY).
     */
    @Column(
            name = "recurring_frequency",
            length = 30
    )
    @Builder.Default
    private String recurringFrequency = "MONTHLY";

    /**
     * Day of month on which recurring expense is due (1-31).
     */
    @Column(name = "recurring_day_of_month")
    private Integer recurringDayOfMonth;

    /**
     * Payment method (e.g. CASH, UPI, BANK_TRANSFER, CARD, CHEQUE).
     */
    @Column(
            name = "payment_method",
            length = 50
    )
    @Builder.Default
    private String paymentMethod = "CASH";

    /**
     * Beneficiary / Landlord / Vendor name paid to.
     */
    @Column(
            name = "paid_to",
            length = 200
    )
    private String paidTo;

    /**
     * Optional receipt/invoice attachment URL (via Cloudinary).
     */
    @Column(
            name = "receipt_url",
            length = 1000
    )
    private String receiptUrl;

    @Column(
            name = "receipt_public_id",
            length = 255
    )
    private String receiptPublicId;

    @Column(
            name = "notes",
            columnDefinition = "TEXT"
    )
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "added_by",
            nullable = false
    )
    private User addedBy;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (expenseDate == null) {
            expenseDate = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (isRecurring && (recurringFrequency == null || recurringFrequency.isBlank())) {
            recurringFrequency = "MONTHLY";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}