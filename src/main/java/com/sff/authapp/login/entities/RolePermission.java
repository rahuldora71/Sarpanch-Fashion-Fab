package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"})
        },
        indexes = {
                @Index(name = "idx_rp_role", columnList = "role_id"),
                @Index(name = "idx_rp_permission", columnList = "permission_id")
        }
)
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_permission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(nullable = false)
    @Builder.Default
    private boolean granted = true;

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    @PreUpdate
    protected void onPersist() {
        this.updatedAt = Instant.now();
    }
}
