package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(
                        name = "idx_user_email",
                        columnList = "user_email"
                ),
                @Index(
                        name = "idx_user_phone",
                        columnList = "phone"
                ),
                @Index(
                        name = "idx_user_customer_code",
                        columnList = "customer_code"
                )
        }
)
public class User implements UserDetails {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;


    // ============================================================
    // BASIC USER INFORMATION
    // ============================================================

    @Column(
            name = "user_email",
            unique = true,
            length = 300
    )
    private String email;

    @Column(
            nullable = false,
            length = 400
    )
    private String name;

    /**
     * Nullable because Google/walk-in customers
     * may not have a local password.
     */
    @Column
    private String password;

    @Column(length = 500)
    private String image;

    @Column(name = "image_public_id", length = 255)
    private String imagePublicId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserImage> userImages = new ArrayList<>();

    /**
     * Worker daily garment production capacity (e.g., 5.00 garments/day).
     */
    @Column(name = "daily_capacity", precision = 8, scale = 2)
    @Builder.Default
    private java.math.BigDecimal dailyCapacity = new java.math.BigDecimal("5.00");

    /**
     * Worker primary production stage / specialty (e.g., STITCHING, CUTTING, IRONING, MASTER_TAILOR).
     */
    @Column(name = "worker_specialty", length = 60)
    @Builder.Default
    private String workerSpecialty = "STITCHING";

    @Column(nullable = false)
    @Builder.Default
    private boolean enable = true;


    // ============================================================
    // AUTHENTICATION PROVIDER
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private Provider provider = Provider.LOCAL;

    @Column(length = 300)
    private String providerId;


    // ============================================================
    // CUSTOMER INFORMATION
    // ============================================================

    /**
     * Phone is NOT unique.
     *
     * Multiple family members may share the same phone number.
     */
    @Column(length = 20)
    private String phone;

    @Column(
            name = "alternate_phone",
            length = 20
    )
    private String alternatePhone;

    /**
     * Business/customer identifier.
     *
     * Example:
     *
     * CUST-000001
     * CUST-000002
     */
    @Column(
            name = "customer_code",
            unique = true,
            length = 30
    )
    private String customerCode;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String notes;


    // ============================================================
    // AUDIT
    // ============================================================

    @Column(
            nullable = false,
            updatable = false
    )
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();


    // ============================================================
    // ROLES
    // ============================================================

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();


    // ============================================================
    // JPA LIFECYCLE
    // ============================================================

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();
    }


    // ============================================================
    // SPRING SECURITY
    // ============================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return roles.stream()
                .map(role ->
                        new SimpleGrantedAuthority(
                                role.getName()
                        )
                )
                .toList();
    }


    @Override
    public String getUsername() {
        return email;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }


    @Override
    public boolean isAccountNonLocked() {
        return true;
    }


    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    @Override
    public boolean isEnabled() {
        return enable;
    }
}