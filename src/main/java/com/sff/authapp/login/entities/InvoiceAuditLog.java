package com.sff.authapp.login.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "invoice_audit_logs",
        indexes = {
                @Index(name = "idx_audit_invoice", columnList = "invoice_id"),
                @Index(name = "idx_audit_time", columnList = "timestamp")
        }
)
public class InvoiceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    /**
     * GENERATED, UPDATED, PAYMENT_RECORDED, CANCELLED, PRINTED, STATUS_CHANGED
     */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
