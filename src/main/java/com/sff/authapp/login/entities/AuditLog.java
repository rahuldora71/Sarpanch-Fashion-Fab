package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(
                        name = "idx_audit_entity",
                        columnList = "entity_name,entity_id"
                ),
                @Index(
                        name = "idx_audit_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_audit_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    @Column(
            name = "action",
            nullable = false,
            length = 50
    )
    private String action;


    @Column(
            name = "entity_name",
            nullable = false,
            length = 100
    )
    private String entityName;


    @Column(
            name = "entity_id",
            nullable = false
    )
    private Long entityId;


    @Column(columnDefinition = "TEXT")
    private String details;


    @Column(name = "ip_address", length = 100)
    private String ipAddress;


    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}