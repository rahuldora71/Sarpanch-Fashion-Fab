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
        name = "worker_settlements",
        indexes = {
                @Index(name = "idx_settlement_worker", columnList = "worker_id"),
                @Index(name = "idx_settlement_date", columnList = "settlement_date")
        }
)
public class WorkerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @Column(name = "settlement_number", length = 50, unique = true)
    private String settlementNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "cycle_label", length = 100)
    private String cycleLabel;

    @Column(name = "total_garments_count")
    private Integer totalGarmentsCount;

    @Column(name = "gross_work_amount", precision = 12, scale = 2)
    private BigDecimal grossWorkAmount;

    @Column(name = "previous_balance", precision = 12, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "total_advances_deducted", precision = 12, scale = 2)
    private BigDecimal totalAdvancesDeducted;

    @Column(name = "net_payable", precision = 12, scale = 2)
    private BigDecimal netPayable;

    @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "closing_balance", precision = 12, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "payment_mode", length = 30)
    private String paymentMode;

    @Column(length = 500)
    private String note;

    @Column(name = "garment_breakdown_json", columnDefinition = "TEXT")
    private String garmentBreakdownJson;

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
