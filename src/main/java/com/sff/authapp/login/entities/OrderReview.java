package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "order_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_review_order",
                        columnNames = "order_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_review_order",
                        columnList = "order_id"
                )
        }
)
public class OrderReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            unique = true
    )
    private Order order;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Size(max = 500)
    @Column(length = 500)
    private String comment;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
