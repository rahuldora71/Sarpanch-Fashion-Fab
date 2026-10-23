package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a shop holiday or special date.
 * Used for capacity planning and delivery-date availability checks.
 */
@Entity
@Table(
        name = "holidays",
        indexes = {
                @Index(
                        name = "idx_holiday_date",
                        columnList = "holiday_date",
                        unique = true
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long id;

    // ============================================================
    // DATE
    // ============================================================

    @Column(
            name = "holiday_date",
            nullable = false
    )
    private LocalDate holidayDate;

    // ============================================================
    // NAME
    // ============================================================

    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    // ============================================================
    // DESCRIPTION
    // ============================================================

    @Column(
            name = "description",
            length = 500
    )
    private String description;

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
     * Capacity available on this holiday (0 to 100).
     * 0  = completely closed
     * 50 = half day
     * 100 = full capacity
     */
    @Column(
            name = "capacity_percentage",
            nullable = false
    )
    @Builder.Default
    private int capacityPercentage = 0;

    // ============================================================
    // RECURRING
    // ============================================================

    /**
     * If true, this holiday repeats every year on the same month/day.
     */
    @Column(
            name = "recurring",
            nullable = false
    )
    @Builder.Default
    private boolean recurring = false;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}