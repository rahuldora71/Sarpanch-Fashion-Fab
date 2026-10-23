package com.sff.authapp.login.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        name = "invoice_items",
        indexes = {
                @Index(name = "idx_item_invoice", columnList = "invoice_id"),
                @Index(name = "idx_item_hsn_sac", columnList = "hsn_sac_code")
        }
)
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @Column(name = "item_order", nullable = false)
    @Builder.Default
    private int itemOrder = 1;

    /**
     * STITCHING, FABRIC, ACCESSORY, ADD_ON, CUSTOM
     */
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private String itemType = "STITCHING";

    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "hsn_sac_code", length = 30)
    private String hsnSacCode;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(length = 20)
    @Builder.Default
    private String unit = "Pcs";

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

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

    @Column(name = "cgst_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "total_tax", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Optional lineage links to source entities
    @Column(name = "garment_id")
    private Long garmentId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "fabric_id")
    private Long fabricId;
}
