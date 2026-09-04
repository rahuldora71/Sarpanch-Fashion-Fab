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
        name = "addons",
        indexes = {
                @Index(
                        name = "idx_addon_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_addon_active",
                        columnList = "active"
                )
        }
)
public class AddOn {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addon_id")
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
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal price;


    // ============================================================
    // TAILOR COMMISSION
    // ============================================================

    /**
     * Default commission earned by the tailor for this add-on.
     *
     * Example:
     *
     * Add-on price      = ₹100
     * Tailor commission = ₹20
     */
    @Column(
            name = "tailor_commission",
            precision = 12,
            scale = 2
    )
    private BigDecimal tailorCommission;


    // ============================================================
    // IMAGE
    // ============================================================

    @Column(
            name = "image_url",
            length = 1000
    )
    private String imageUrl;


    @Column(
            name = "image_public_id",
            length = 500
    )
    private String imagePublicId;


    // ============================================================
    // CUTTING
    // ============================================================

    @Column(
            name = "before_cutting",
            nullable = false
    )
    @Builder.Default
    private boolean beforeCutting = true;


    // ============================================================
    // STATUS
    // ============================================================

    @Column(
            nullable = false
    )
    @Builder.Default
    private boolean active = true;


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