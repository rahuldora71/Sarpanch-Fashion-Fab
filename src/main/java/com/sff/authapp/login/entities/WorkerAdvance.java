package com.sff.authapp.login.entities;

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
        name = "worker_advances",
        indexes = {
                @Index(name = "idx_worker_advance_worker", columnList = "worker_id"),
                @Index(name = "idx_worker_advance_date", columnList = "advance_date")
        }
)
public class WorkerAdvance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String type = "DAILY_KHARCHA"; // DAILY_KHARCHA, ADVANCE, MATERIAL, OTHER

    @Column(length = 500)
    private String note;

    @Column(name = "payment_mode", length = 30, nullable = false)
    @Builder.Default
    private String paymentMode = "CASH"; // CASH, UPI, BANK_TRANSFER

    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
