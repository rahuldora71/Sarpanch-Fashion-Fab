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
        name = "service_images",
        indexes = {
                @Index(
                        name = "idx_service_image_service",
                        columnList = "service_id"
                )
        }
)
public class ServiceImage {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_image_id")
    private Long id;


    // ============================================================
    // SERVICE
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "service_id",
            nullable = false
    )
    private TailoringService service;


    // ============================================================
    // IMAGE
    // ============================================================

    @Column(
            name = "service_image_url",
            nullable = false,
            length = 1000
    )
    private String imageUrl;


    // ============================================================
    // AUDIT
    // ============================================================

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
}