package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "garment_measurements",
        indexes = {

                @Index(
                        name = "idx_measurement_garment",
                        columnList = "garment_id"
                ),

                @Index(
                        name = "idx_measurement_service",
                        columnList = "service_id"
                ),

                @Index(
                        name = "idx_measurement_user",
                        columnList = "user_id"
                )
        }
)
public class GarmentMeasurement {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "measurement_id")
    private Long id;


    // ============================================================
    // GARMENT
    // ============================================================

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "garment_id",
            nullable = false,
            unique = true
    )
    private Garment garment;


    // ============================================================
    // SERVICE
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private TailoringService service;


    // ============================================================
    // TAKEN BY
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taken_by")
    private User takenBy;


    // ============================================================
    // CUSTOMER
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    // ============================================================
    // MEASUREMENT VALUES
    // ============================================================

    /**
     * Actual measurement snapshot for this garment.
     *
     * Values are keyed by Field ID.
     *
     * Example:
     *
     * {
     *     "1": 38,
     *     "2": 17,
     *     "3": 24.5
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "measurement_fields",
            columnDefinition = "json"
    )
    @Builder.Default
    private Map<String, Object> measurementFields =
            new LinkedHashMap<>();


    // ============================================================
    // ALTERATION
    // ============================================================

    /**
     * Alteration values are also keyed by Field ID.
     *
     * Example:
     *
     * {
     *     "1": 1.0,
     *     "3": -1.5
     * }
     *
     * Positive = increase
     * Negative = decrease
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "alteration",
            columnDefinition = "json"
    )
    @Builder.Default
    private Map<String, Object> alteration =
            new LinkedHashMap<>();


    // ============================================================
    // NOTES
    // ============================================================

    @Column(columnDefinition = "TEXT")
    private String note;


    @Column(
            name = "alteration_note",
            columnDefinition = "TEXT"
    )
    private String alterationNote;


    // ============================================================
    // STATE
    // ============================================================

    @Column(
            nullable = false
    )
    @Builder.Default
    private boolean editable = true;


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