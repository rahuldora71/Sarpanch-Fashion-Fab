package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        name = "garments",
        indexes = {
                @Index(
                        name = "idx_garment_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_garment_service",
                        columnList = "service_id"
                ),
                @Index(
                        name = "idx_garment_measurement",
                        columnList = "measurement_id"
                ),
                @Index(
                        name = "idx_garment_status",
                        columnList = "status"
                )
        }
)
public class Garment {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "garment_id")
    private Long id;


    // ============================================================
    // ORDER
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private Order order;


    // ============================================================
    // SERVICE
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "service_id",
            nullable = false
    )
    private TailoringService service;


    // ============================================================
    // GARMENT PRICE
    // ============================================================

    /**
     * Price of this garment/service at the time
     * the order was created.
     *
     * Do not depend on the current service price.
     */
    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal price;


    // ============================================================
    // SAMPLE
    // ============================================================

    @Column(
            name = "sample_number",
            length = 50
    )
    private String sampleNumber;


    // ============================================================
    // MEASUREMENT
    // ============================================================

    /**
     * Measurement used by this garment.
     *
     * Multiple garments are allowed to reference
     * the same GarmentMeasurement.
     *
     * Example:
     *
     * Garment 101 -> Measurement 25
     * Garment 102 -> Measurement 25
     * Garment 103 -> Measurement 30
     *
     * This allows "Copy Latest Measurement" without
     * creating duplicate measurement records.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "measurement_id"
    )
    private GarmentMeasurement garmentMeasurement;


    // ============================================================
    // ADD-ONS
    // ============================================================

    @OneToMany(
            mappedBy = "garment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<GarmentAddOn> addOns = new ArrayList<>();


    // ============================================================
    // TRACKING
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private GarmentStatus status =
            GarmentStatus.FABRIC_CUT;


    // ============================================================
    // FABRIC
    // ============================================================

    /**
     * true  -> customer provided fabric
     * false -> shop provided fabric
     */
    @Column(
            name = "customer_fabric",
            nullable = false
    )
    @Builder.Default
    private boolean customerFabric = true;


    // ============================================================
    // DATES
    // ============================================================

    @Column(name = "try_date")
    private LocalDate tryDate;


    @Column(name = "due_date")
    private LocalDate dueDate;


    // ============================================================
    // WORKERS
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cutting_by")
    private User cuttingBy;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "made_by")
    private User madeBy;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivered_by")
    private User deliveredBy;


    // ============================================================
    // ALTERATION
    // ============================================================

    @Column(name = "alteration_delivery_date")
    private LocalDate alterationDeliveryDate;


    @Column(
            name = "alteration_note",
            columnDefinition = "TEXT"
    )
    private String alterationNote;


    // ============================================================
    // NOTE
    // ============================================================

    @Column(
            columnDefinition = "TEXT"
    )
    private String note;


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