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
        name = "fabrics",
        indexes = {
                @Index(name = "idx_fabric_code", columnList = "fabric_code", unique = true),
                @Index(name = "idx_fabric_active", columnList = "active")
        }
)
public class Fabric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fabric_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "fabric_code", nullable = false, unique = true, length = 50)
    private String fabricCode;

    /**
     * Cotton, Linen, Wool, Silk, Polyester, Blend, Terry Cotton, etc.
     */
    @Column(name = "fabric_type", length = 80)
    private String fabricType;

    @Column(length = 50)
    private String color;

    @Column(name = "width_inches", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal widthInches = new BigDecimal("44.00");

    @Column(name = "stock_meters", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal stockMeters = BigDecimal.ZERO;

    @Column(name = "cost_price_per_meter", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costPricePerMeter = BigDecimal.ZERO;

    @Column(name = "selling_price_per_meter", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sellingPricePerMeter = BigDecimal.ZERO;

    @Column(name = "hsn_code", length = 30)
    @Builder.Default
    private String hsnCode = "5208";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rule_id")
    private TaxRule taxRule;

    @Column(name = "min_stock_alert", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minStockAlert = new BigDecimal("10.00");

    @Column(length = 255)
    private String supplier;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
