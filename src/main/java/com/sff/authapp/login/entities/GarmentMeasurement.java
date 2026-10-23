package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
                        name = "idx_measurement_service",
                        columnList = "service_id"
                ),

                @Index(
                        name = "idx_measurement_user",
                        columnList = "user_id"
                ),

                @Index(
                        name = "idx_measurement_taken_by",
                        columnList = "taken_by_user_id"
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
    // SERVICE
    // ============================================================

    /**
     * Tailoring service for which this measurement was taken.
     *
     * Example:
     *
     * Measurement 25 -> Shirt
     * Measurement 30 -> Pant
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "service_id",
            nullable = false
    )
    private TailoringService service;


    // ============================================================
    // TAKEN BY
    // ============================================================

    /**
     * User/staff member who took the measurement.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "taken_by_user_id",
            nullable = false
    )
    private User takenBy;


    // ============================================================
    // CUSTOMER
    // ============================================================

    /**
     * Customer whose measurement this is.
     *
     * A measurement can be reused by multiple garments
     * belonging to this customer.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    // ============================================================
    // MEASUREMENT VALUES
    // ============================================================

    /**
     * Actual measurement snapshot.
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
     *
     * This record should be treated as a snapshot.
     *
     * If the customer's measurements change later,
     * create a new GarmentMeasurement instead of modifying
     * an old measurement used by previous orders.
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

    @Column(
            columnDefinition = "TEXT"
    )
    private String note;


    @Column(
            name = "alteration_note",
            columnDefinition = "TEXT"
    )
    private String alterationNote;


    // ============================================================
    // STATE
    // ============================================================

    /**
     * Whether this measurement can currently be edited.
     *
     * Existing orders that reference this measurement
     * should normally use the snapshot without modification.
     */
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

        LocalDateTime now =
                LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}