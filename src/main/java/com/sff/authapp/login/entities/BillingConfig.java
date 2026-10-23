package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "billing_configs")
public class BillingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long id;

    @Column(name = "shop_name", nullable = false, length = 150)
    @Builder.Default
    private String shopName = "Sarpanch Fashion Fab";

    @Column(name = "tagline", length = 200)
    @Builder.Default
    private String tagline = "Custom Tailoring & Designer Fabrics";

    @Column(name = "gstin", length = 30)
    @Builder.Default
    private String gstin = "08AAAAA0000A1Z5";

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "state", length = 100)
    @Builder.Default
    private String state = "Rajasthan";

    @Column(name = "state_code", length = 10)
    @Builder.Default
    private String stateCode = "08";

    @Column(length = 255)
    @Builder.Default
    private String address = "Main Market, Tailor Street, Jaipur, Rajasthan";

    @Column(length = 25)
    @Builder.Default
    private String phone = "+91 98765 43210";

    @Column(length = 150)
    private String email;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 30)
    private String ifscCode;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "invoice_prefix", length = 20)
    @Builder.Default
    private String invoicePrefix = "SFF";

    @Column(name = "financial_year", length = 20)
    @Builder.Default
    private String financialYear = "2026-27";

    // switches
    @Column(name = "default_tax_inclusive", nullable = false)
    @Builder.Default
    private boolean defaultTaxInclusive = false;

    @Column(name = "include_stitching_only", nullable = false)
    @Builder.Default
    private boolean includeStitchingOnly = true;

    @Column(name = "allow_mixed_orders", nullable = false)
    @Builder.Default
    private boolean allowMixedOrders = true;

    @Column(name = "allow_fabric_only", nullable = false)
    @Builder.Default
    private boolean allowFabricOnly = true;

    @Column(name = "allow_manual_invoices", nullable = false)
    @Builder.Default
    private boolean allowManualInvoices = true;

    @Column(name = "allow_custom_line_items", nullable = false)
    @Builder.Default
    private boolean allowCustomLineItems = true;

    @Column(name = "allow_partial_billing", nullable = false)
    @Builder.Default
    private boolean allowPartialBilling = true;

    @Column(name = "enforce_sequential_numbering", nullable = false)
    @Builder.Default
    private boolean enforceSequentialNumbering = true;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    @Builder.Default
    private String termsAndConditions = "1. Goods once sold will not be exchanged or returned without bill.\n2. Fitting / alteration complaints accepted within 7 days of delivery.\n3. Unclaimed garments after 90 days are not the responsibility of shop management.";

    @Column(name = "footer_note", columnDefinition = "TEXT")
    @Builder.Default
    private String footerNote = "Thank you for your business! Visit us again.";

    /**
     * A4, THERMAL
     */
    @Column(name = "default_invoice_template", length = 30)
    @Builder.Default
    private String defaultInvoiceTemplate = "A4";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
