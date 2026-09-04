package com.sff.authapp.login.entities;

import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(
                        name = "idx_payment_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_payment_paid_at",
                        columnList = "paid_at"
                ),
                @Index(
                        name = "idx_payment_type",
                        columnList = "payment_type"
                )
        }
)
public class Payment {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;


    // ============================================================
    // ORDER
    // ============================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private Order order;


    // ============================================================
    // PAYMENT AMOUNT
    // ============================================================

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;


    // ============================================================
    // PAYMENT METHOD
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 30
    )
    private PaymentMethod paymentMethod;


    // ============================================================
    // PAYMENT TYPE
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_type",
            nullable = false,
            length = 30
    )
    private PaymentType paymentType;


    // ============================================================
    // PAYMENT DATE
    // ============================================================

    @Column(
            name = "paid_at",
            nullable = false
    )
    private LocalDateTime paidAt;


    // ============================================================
    // NOTE
    // ============================================================

    @Column(length = 500)
    private String note;


    // ============================================================
    // RECEIVED BY
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id")
    private User receivedBy;


    // ============================================================
    // AUDIT
    // ============================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    // ============================================================
    // JPA LIFECYCLE
    // ============================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (paidAt == null) {
            paidAt = now;
        }
    }
}