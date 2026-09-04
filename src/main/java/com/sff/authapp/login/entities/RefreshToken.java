package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(
                        name = "refresh_tokens_jti_idx",
                        columnList = "jti",
                        unique = true
                ),
                @Index(
                        name = "refresh_tokens_user_id_idx",
                        columnList = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(
            name = "jti",
            nullable = false,
            unique = true,
            length = 100
    )
    private String jti;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private User user;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    @Column(
            nullable = false
    )
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "replaced_by_token", length = 100)
    private String replacedByToken;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}