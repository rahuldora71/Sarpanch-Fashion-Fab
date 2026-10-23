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
        name = "audit_access_logs",
        indexes = {
                @Index(name = "idx_aal_actor", columnList = "actor_email"),
                @Index(name = "idx_aal_timestamp", columnList = "timestamp")
        }
)
public class AuditAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", length = 120)
    private String actorEmail;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "target", length = 150)
    private String target;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
