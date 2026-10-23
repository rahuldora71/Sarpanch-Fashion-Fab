package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Production capacity configuration for daily production planning.
 * Allows setting different capacities for different production stages.
 */
@Entity
@Table(
        name = "production_capacity",
        indexes = {
                @Index(
                        name = "idx_capacity_date",
                        columnList = "capacity_date"
                ),
                @Index(
                        name = "idx_capacity_stage",
                        columnList = "production_stage"
                ),
                @Index(
                        name = "idx_capacity_worker",
                        columnList = "worker_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capacity_id")
    private Long id;

    // ============================================================
    // DATE
    // ============================================================

    @Column(
            name = "capacity_date",
            nullable = false
    )
    private LocalDate capacityDate;

    // ============================================================
    // PRODUCTION STAGE
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "production_stage",
            nullable = false,
            length = 30
    )
    private ProductionStage productionStage;

    // ============================================================
    // WORKER (optional - for worker-specific capacity)
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    // ============================================================
    // CAPACITY
    // ============================================================

    @Column(
            name = "max_capacity",
            nullable = false,
            precision = 8,
            scale = 2
    )
    private BigDecimal maxCapacity;

    @Column(
            name = "available_capacity",
            nullable = false,
            precision = 8,
            scale = 2
    )
    @Builder.Default
    private BigDecimal availableCapacity = BigDecimal.ZERO;

    // ============================================================
    // STATUS
    // ============================================================

    @Column(
            nullable = false
    )
    @Builder.Default
    private boolean active = true;

    @Column(
            name = "capacity_reduced",
            nullable = false
    )
    @Builder.Default
    private boolean capacityReduced = false;

    @Column(
            name = "reduction_reason",
            length = 500
    )
    private String reductionReason;

    // ============================================================
    // AUDIT
    // ============================================================

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

    // ============================================================
    // JPA LIFECYCLE
    // ============================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;

        // Initialize available capacity to max capacity
        if (availableCapacity == null) {
            availableCapacity = maxCapacity;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ============================================================
    // PRODUCTION STAGE ENUM
    // ============================================================

    public enum ProductionStage {
        CUTTING,
        STITCHING,
        IRONING,
        FINISHING,
        QUALITY_CHECK,
        ALTERATION,
        WASHING
    }
}