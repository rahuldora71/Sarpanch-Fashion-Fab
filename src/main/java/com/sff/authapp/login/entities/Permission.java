package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "permissions",
        indexes = {
                @Index(name = "idx_perm_key", columnList = "permission_key", unique = true),
                @Index(name = "idx_perm_resource", columnList = "resource_id")
        }
)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String permissionKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_id", nullable = false)
    private AppResource resource;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 255)
    private String description;
}
