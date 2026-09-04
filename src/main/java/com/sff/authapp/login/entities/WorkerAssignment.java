package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "worker_assignments",
        indexes = {
                @Index(
                        name = "idx_assignment_garment",
                        columnList = "garment_id"
                ),
                @Index(
                        name = "idx_assignment_worker",
                        columnList = "worker_id"
                ),
                @Index(
                        name = "idx_assignment_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_assignment_work_type",
                        columnList = "work_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "garment_id",
            nullable = false
    )
    private Garment garment;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "worker_id",
            nullable = false
    )
    private User worker;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "work_type",
            nullable = false,
            length = 30
    )
    private WorkType workType;


    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private Status status = Status.ASSIGNED;


    @Column(
            name = "assigned_at",
            nullable = false
    )
    private LocalDateTime assignedAt;


    @Column(name = "started_at")
    private LocalDateTime startedAt;


    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @Column(columnDefinition = "TEXT")
    private String notes;


    @PrePersist
    protected void onCreate() {

        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }


    public enum WorkType {

        CUTTING,
        STITCHING,
        ALTERATION,
        FINISHING,
        IRONING,
        QUALITY_CHECK,
        OTHER
    }


    public enum Status {

        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}