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
        name = "month_end_closings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_closing_month_year", columnNames = {"closing_month", "closing_year"})
        }
)
public class MonthEndClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "closing_id")
    private Long id;

    @Column(name = "closing_month", nullable = false)
    private int closingMonth;

    @Column(name = "closing_year", nullable = false)
    private int closingYear;

    @Column(name = "total_invoices", nullable = false)
    @Builder.Default
    private int totalInvoices = 0;

    @Column(name = "total_taxable_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxableAmount = BigDecimal.ZERO;

    @Column(name = "total_cgst", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCgst = BigDecimal.ZERO;

    @Column(name = "total_sgst", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSgst = BigDecimal.ZERO;

    @Column(name = "total_igst", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalIgst = BigDecimal.ZERO;

    @Column(name = "total_turnover", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalTurnover = BigDecimal.ZERO;

    @Column(name = "cancelled_invoices_count", nullable = false)
    @Builder.Default
    private int cancelledInvoicesCount = 0;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (closedAt == null) {
            closedAt = LocalDateTime.now();
        }
    }
}
