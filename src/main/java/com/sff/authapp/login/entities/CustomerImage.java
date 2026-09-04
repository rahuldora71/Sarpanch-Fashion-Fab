package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "customer_images",
        indexes = {
                @Index(
                        name = "idx_customer_image_user",
                        columnList = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "image_url",
            nullable = false,
            length = 1000
    )
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "image_type",
            nullable = false,
            length = 30
    )
    private ImageType imageType;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ImageType {
        FRONT,
        BACK,
        SIDE,
        OTHER
    }
}