package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "tax_rules",
        indexes = {
                @Index(name = "idx_tax_rule_code", columnList = "hsn_sac_code"),
                @Index(name = "idx_tax_rule_active", columnList = "active")
        }
)
public class TaxRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tax_rule_id")
    private Long id;

    @Column(name = "tax_name", nullable = false, length = 150)
    private String taxName;

    /**
     * GST, EXEMPT, NIL_RATED, COMPOSITION
     */
    @Column(name = "tax_type", nullable = false, length = 50)
    @Builder.Default
    private String taxType = "GST";

    @Column(name = "gst_rate", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(name = "cgst_rate", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal cgstRate = BigDecimal.ZERO;

    @Column(name = "sgst_rate", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal sgstRate = BigDecimal.ZERO;

    @Column(name = "igst_rate", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal igstRate = BigDecimal.ZERO;

    @Column(name = "hsn_sac_code", length = 30)
    private String hsnSacCode;

    /**
     * STITCHING, FABRIC, ACCESSORY, GENERAL
     */
    @Column(name = "category", length = 50)
    @Builder.Default
    private String category = "GENERAL";

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(columnDefinition = "TEXT")
    private String description;

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
