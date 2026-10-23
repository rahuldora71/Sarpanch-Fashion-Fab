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
        name = "app_resources",
        indexes = {
                @Index(name = "idx_resource_key", columnList = "resource_key", unique = true)
        }
)
public class AppResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long id;

    @Column(name = "resource_key", nullable = false, unique = true, length = 60)
    private String resourceKey;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 80)
    private String category;

    @Column(length = 150)
    private String route;

    @Column(length = 255)
    private String description;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
