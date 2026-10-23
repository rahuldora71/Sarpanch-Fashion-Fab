package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_access_logs",
        indexes = {
                @Index(name = "idx_acc_order", columnList = "order_id"),
                @Index(name = "idx_acc_token", columnList = "tracking_token"),
                @Index(name = "idx_acc_date", columnList = "accessed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_number")
    private Long orderNumber;

    @Column(name = "tracking_token", length = 64)
    private String trackingToken;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * "MOBILE", "TABLET", "DESKTOP", "UNKNOWN"
     */
    @Column(name = "device_type", length = 30)
    private String deviceType;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    @PrePersist
    protected void onCreate() {
        if (accessedAt == null) {
            accessedAt = LocalDateTime.now();
        }
        if (deviceType == null) {
            deviceType = "MOBILE";
        }
    }
}
