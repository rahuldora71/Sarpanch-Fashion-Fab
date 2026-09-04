package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "garment_addons",
        indexes = {
                @Index(
                        name = "idx_garment_addon_garment",
                        columnList = "garment_id"
                ),
                @Index(
                        name = "idx_garment_addon_addon",
                        columnList = "addon_id"
                )
        }
)
public class GarmentAddOn {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "garment_addon_id")
    private Long id;


    // ============================================================
    // GARMENT
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "garment_id",
            nullable = false
    )
    private Garment garment;


    // ============================================================
    // ADD-ON
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "addon_id",
            nullable = false
    )
    private AddOn addOn;


    // ============================================================
    // SNAPSHOT INFORMATION
    // ============================================================

    /**
     * Name copied from AddOn at order time.
     *
     * This protects historical orders if the AddOn
     * name is changed later.
     */
    @Column(
            name = "addon_name",
            nullable = false,
            length = 150
    )
    private String addOnName;


    /**
     * Price of one AddOn at order time.
     */
    @Column(
            name = "unit_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal unitPrice;


    // ============================================================
    // QUANTITY
    // ============================================================

    @Column(
            nullable = false
    )
    @Builder.Default
    private Integer quantity = 1;


    // ============================================================
    // TOTAL
    // ============================================================

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalPrice;


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
            name = "tailor_commission",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal tailorCommission;

    // ============================================================
    // JPA LIFECYCLE
    // ============================================================

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );
        }
    }


    @PreUpdate
    protected void onUpdate() {

        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );
        }
    }
}