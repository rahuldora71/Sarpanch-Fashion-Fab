package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "tailoring_services",
        indexes = {
                @Index(
                        name = "idx_service_category",
                        columnList = "category"
                ),
                @Index(
                        name = "idx_service_active",
                        columnList = "active"
                )
        }
)
public class TailoringService {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long id;


    // ============================================================
    // BASIC INFORMATION
    // ============================================================

    @Column(
            nullable = false,
            length = 150
    )
    private String name;


    @Column(
            length = 100
    )
    private String category;


    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal price;


    @Column(
            nullable = false
    )
    @Builder.Default
    private boolean active = true;


    // ============================================================
    // TAILOR COMMISSION
    // ============================================================

    @Column(
            name = "tailor_commission",
            precision = 12,
            scale = 2
    )
    private BigDecimal tailorCommission;


    // ============================================================
    // MEASUREMENT TEMPLATE
    // ============================================================

    /**
     * Example:
     *
     * {
     *   "fields": [
     *     {
     *       "key": "chest",
     *       "label": "Chest",
     *       "unit": "INCH",
     *       "required": true
     *     },
     *     {
     *       "key": "shoulder",
     *       "label": "Shoulder",
     *       "unit": "INCH",
     *       "required": true
     *     }
     *   ]
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "measurement_fields",
            columnDefinition = "json"
    )
    private List<MeasurementField> measurementFields;


    // ============================================================
    // SERVICE IMAGES
    // ============================================================

    @OneToMany(
            mappedBy = "service",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ServiceImage> images = new ArrayList<>();


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
    // MEASUREMENT FIELD
    // ============================================================


}