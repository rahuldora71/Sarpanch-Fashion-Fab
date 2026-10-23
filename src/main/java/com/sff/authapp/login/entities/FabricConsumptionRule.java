package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "fabric_consumption_rules",
        indexes = {
                @Index(name = "idx_fcr_garment_type", columnList = "garment_type")
        }
)
public class FabricConsumptionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    /**
     * SHIRT, PANT, SUIT_2PC, SUIT_3PC, KURTA, PAJAMA, WAISTCOAT, SAFARI, SHERWANI, BLAZER
     */
    @Column(name = "garment_type", nullable = false, length = 60)
    private String garmentType;

    /**
     * Fabric width in inches, e.g. 36.00, 44.00, 58.00
     */
    @Column(name = "width_inches", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal widthInches = new BigDecimal("44.00");

    /**
     * Standard meters required for standard size
     */
    @Column(name = "base_consumption_meters", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal baseConsumptionMeters = new BigDecimal("1.60");

    /**
     * Expected cutting wastage percentage (e.g. 5.00%)
     */
    @Column(name = "wastage_percent", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal wastagePercent = new BigDecimal("5.00");

    @Column(length = 255)
    private String description;
}
