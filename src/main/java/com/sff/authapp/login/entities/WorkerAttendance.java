package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks daily worker attendance.
 * Affects production capacity calculations.
 */
@Entity
@Table(
        name = "worker_attendance",
        indexes = {
                @Index(
                        name = "idx_attendance_worker_date",
                        columnList = "worker_id, attendance_date",
                        unique = true
                ),
                @Index(
                        name = "idx_attendance_date",
                        columnList = "attendance_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    // ============================================================
    // WORKER
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "worker_id",
            nullable = false
    )
    private User worker;

    // ============================================================
    // DATE
    // ============================================================

    @Column(
            name = "attendance_date",
            nullable = false
    )
    private LocalDate attendanceDate;

    // ============================================================
    // STATUS
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    // ============================================================
    // NOTE
    // ============================================================

    @Column(
            name = "note",
            length = 500
    )
    private String note;

    // ============================================================
    // CAPACITY IMPACT PERCENTAGE
    // ============================================================

    /**
     * Percentage of this worker's capacity contributed on this date.
     * PRESENT  = 100
     * HALF_DAY = 50
     * ABSENT   = 0
     * LEAVE    = 0
     */
    @Column(
            name = "capacity_percentage",
            nullable = false
    )
    @Builder.Default
    private int capacityPercentage = 100;

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

    // ============================================================
    // ATTENDANCE STATUS ENUM
    // ============================================================

    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        HALF_DAY,
        LEAVE
    }
}