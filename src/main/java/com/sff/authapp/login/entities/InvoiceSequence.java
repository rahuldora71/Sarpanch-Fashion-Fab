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
@Table(
        name = "invoice_sequences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_prefix_fy", columnNames = {"prefix", "financial_year"})
        }
)
public class InvoiceSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id")
    private Long id;

    @Column(nullable = false, length = 30)
    private String prefix;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "current_sequence", nullable = false)
    @Builder.Default
    private Long currentSequence = 0L;

    @Column(nullable = false)
    @Builder.Default
    private int padding = 4;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
