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
        name = "invoices",
        indexes = {
                @Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true),
                @Index(name = "idx_invoice_date", columnList = "invoice_date"),
                @Index(name = "idx_invoice_status", columnList = "status"),
                @Index(name = "idx_invoice_classification", columnList = "classification"),
                @Index(name = "idx_invoice_customer_phone", columnList = "customer_phone")
        }
)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * TAX_INVOICE, BILL_OF_SUPPLY, SERVICE_INVOICE, FABRIC_SALE, MIXED_INVOICE, CUSTOM_INVOICE
     */
    @Column(name = "invoice_type", nullable = false, length = 40)
    @Builder.Default
    private String invoiceType = "TAX_INVOICE";

    /**
     * STITCHING_ONLY, FABRIC_ONLY, FABRIC_AND_STITCHING, SERVICE_ONLY, MIXED
     */
    @Column(name = "classification", nullable = false, length = 40)
    @Builder.Default
    private String classification = "STITCHING_ONLY";

    /**
     * DRAFT, GENERATED, ISSUED, PAID, PARTIALLY_PAID, CANCELLED
     */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "GENERATED";

    @Column(name = "is_tax_inclusive", nullable = false)
    @Builder.Default
    private boolean isTaxInclusive = false;

    @Column(name = "place_of_supply", length = 50)
    @Builder.Default
    private String placeOfSupply = "08 - Rajasthan";

    @Column(name = "is_inter_state", nullable = false)
    @Builder.Default
    private boolean isInterState = false;

    // Customer association & snapshots
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 25)
    private String customerPhone;

    @Column(name = "customer_address", length = 300)
    private String customerAddress;

    @Column(name = "customer_gstin", length = 30)
    private String customerGstin;

    @Column(name = "customer_state_code", length = 10)
    private String customerStateCode;

    // Financial totals (decimal safe)
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

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

    @Column(name = "round_off", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    /**
     * UNPAID, PARTIALLY_PAID, PAID
     */
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    /**
     * CASH, UPI, CARD, BANK_TRANSFER, CHEQUE, MIXED
     */
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    // Cancellation audit
    @Column(name = "is_cancelled", nullable = false)
    @Builder.Default
    private boolean isCancelled = false;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceOrder> invoiceOrders = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentDate DESC")
    @Builder.Default
    private List<InvoicePayment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp DESC")
    @Builder.Default
    private List<InvoiceAuditLog> auditLogs = new ArrayList<>();

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
