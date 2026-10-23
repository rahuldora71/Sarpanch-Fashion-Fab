package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(
                        name = "idx_order_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_order_date",
                        columnList = "order_date"
                ),
                @Index(
                        name = "idx_order_number",
                        columnList = "order_number"
                ),
                @Index(
                        name = "idx_order_tracking_token",
                        columnList = "tracking_token",
                        unique = true
                )
        }
)
public class Order {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;


    // ============================================================
    // CUSTOMER
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private User customer;


    // ============================================================
    // ORDER INFORMATION
    // ============================================================

    @Column(
            name = "order_date",
            nullable = false
    )
    private LocalDate orderDate;

    @Column(name = "order_number")
    private Long orderNumber;

    @Column(name = "bill_image_url", length = 1000)
    private String billImageUrl;

    @Column(name = "bill_image_public_id", length = 255)
    private String billImagePublicId;

    @Column(name = "is_delivered")
    @Builder.Default
    private Boolean isDelivered = false;

    @Column(
            name = "sample_book_page_number",
            length = 50
    )
    private String sampleBookPageNumber;

    @Column(
            name = "tracking_token",
            length = 64,
            unique = true
    )
    private String trackingToken;


    // ============================================================
    // GARMENTS
    // ============================================================

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    @Builder.Default
    private List<Garment> garments = new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AdditionalCharge> additionalCharges;


    BigDecimal discount;
    // ============================================================
    // AUDIT
    // ============================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;



    // ============================================================
    // JPA LIFECYCLE
    // ============================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (trackingToken == null || trackingToken.isBlank()) {
            trackingToken = "sff_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        updatedAt = now;
    }


    @PreUpdate
    protected void onUpdate() {

        if (trackingToken == null || trackingToken.isBlank()) {
            trackingToken = "sff_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        updatedAt = LocalDateTime.now();
    }

    public String getEffectiveTrackingToken() {
        if (trackingToken == null || trackingToken.isBlank()) {
            trackingToken = "sff_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        return trackingToken;
    }
}
