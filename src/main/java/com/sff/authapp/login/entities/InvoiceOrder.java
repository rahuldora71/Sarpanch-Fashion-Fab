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
        name = "invoice_orders",
        indexes = {
                @Index(name = "idx_inv_order_inv", columnList = "invoice_id"),
                @Index(name = "idx_inv_order_ord", columnList = "order_id")
        }
)
public class InvoiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "billed_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal billedAmount = BigDecimal.ZERO;

    @Column(name = "is_partial", nullable = false)
    @Builder.Default
    private boolean isPartial = false;
}
