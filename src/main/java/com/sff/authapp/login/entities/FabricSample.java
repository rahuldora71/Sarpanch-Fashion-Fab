package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fabric_samples",
        indexes = {
                @Index(
                        name = "idx_fabric_sample_code",
                        columnList = "sample_code",
                        unique = true
                ),
                @Index(
                        name = "idx_fabric_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_fabric_order",
                        columnList = "order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabricSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "sample_code",
            nullable = false,
            unique = true,
            length = 30
    )
    private String sampleCode;


    @Column(
            name = "book_number",
            nullable = false,
            length = 20
    )
    private String bookNumber;


    @Column(
            name = "page_number",
            nullable = false,
            length = 20
    )
    private String pageNumber;


    @Column(length = 100)
    private String color;


    @Column(length = 255)
    private String description;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User customer;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;


    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private Status status = Status.AVAILABLE;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    public enum Status {

        AVAILABLE,
        IN_PRODUCTION,
        COMPLETED,
        CANCELLED
    }
}