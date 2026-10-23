package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A date that is locked for delivery scheduling.
 * Orders cannot be assigned a delivery date on a blocked date.
 */
@Entity
@Table(
        name = "blocked_dates",
        indexes = {
                @Index(
                        name = "idx_blocked_date",
                        columnList = "blocked_date",
                        unique = true
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blocked_date_id")
    private Long id;

    // ============================================================
    // DATE
    // ============================================================

    @Column(
            name = "blocked_date",
            nullable = false
    )
    private LocalDate blockedDate;

    // ============================================================
    // REASON
    // ============================================================

    @Column(
            name = "reason",
            length = 500
    )
    private String reason;

    // ============================================================
    // DELIVERY ALLOWED
    // ============================================================

    @Column(
            name = "delivery_allowed",
            nullable = false
    )
    @Builder.Default
    private boolean deliveryAllowed = false;

    // ============================================================
    // WORKERS WORKING
    // ============================================================

    @Column(
            name = "workers_working",
            nullable = false
    )
    @Builder.Default
    private boolean workersWorking = false;

    // ============================================================
    // CAPACITY PERCENTAGE
    // ============================================================

    /**
     * Capacity percentage on this date (0-100).
     * 0 means closed, 100 means full capacity.
     */
    @Column(
            name = "capacity_percentage",
            nullable = false
    )
    @Builder.Default
    private int capacityPercentage = 0;

    // ============================================================
    // AUDIT
    // ============================================================

    @Column(
            name = "created_by",
            nullable = false
    )
    private Long createdBy;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}