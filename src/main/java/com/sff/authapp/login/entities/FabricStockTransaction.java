package com.sff.authapp.login.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        name = "fabric_stock_transactions",
        indexes = {
                @Index(name = "idx_fst_fabric", columnList = "fabric_id"),
                @Index(name = "idx_fst_type", columnList = "transaction_type"),
                @Index(name = "idx_fst_date", columnList = "transaction_date")
        }
)
public class FabricStockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fabric_id", nullable = false)
    @JsonIgnore
    private Fabric fabric;

    /**
     * PURCHASE, SALE, ORDER_CONSUMPTION, ADJUSTMENT, RETURN, WASTAGE
     */
    @Column(name = "transaction_type", nullable = false, length = 35)
    private String transactionType;

    /**
     * Quantity change (+ positive for stock addition, - negative for stock consumption/sale)
     */
    @Column(name = "quantity_meters", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityMeters;

    @Column(name = "previous_stock", nullable = false, precision = 10, scale = 2)
    private BigDecimal previousStock;

    @Column(name = "new_stock", nullable = false, precision = 10, scale = 2)
    private BigDecimal newStock;

    @Column(name = "reference_no", length = 60)
    private String referenceNo;

    /**
     * INVOICE, ORDER, MANUAL, PO
     */
    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(length = 255)
    private String notes;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
}
