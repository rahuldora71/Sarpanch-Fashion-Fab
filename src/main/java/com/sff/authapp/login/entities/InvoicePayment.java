package com.sff.authapp.login.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "invoice_payments",
        indexes = {
                @Index(name = "idx_inv_pay_inv", columnList = "invoice_id"),
                @Index(name = "idx_inv_pay_date", columnList = "payment_date")
        }
)
public class InvoicePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * CASH, UPI, CARD, BANK_TRANSFER, CHEQUE
     */
    @Column(name = "payment_method", nullable = false, length = 30)
    @Builder.Default
    private String paymentMethod = "CASH";

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(length = 255)
    private String notes;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
    }
}
