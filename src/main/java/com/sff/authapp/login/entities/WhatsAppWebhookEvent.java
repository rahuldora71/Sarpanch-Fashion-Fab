package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log and event store for all incoming Meta WhatsApp webhook events.
 * Provides durability, traceability, and idempotency guarantees.
 */
@Entity
@Table(
        name = "whatsapp_webhook_events",
        indexes = {
                @Index(name = "idx_wa_event_wamid", columnList = "provider_message_id"),
                @Index(name = "idx_wa_event_type", columnList = "event_type"),
                @Index(name = "idx_wa_event_status", columnList = "status"),
                @Index(name = "idx_wa_event_received", columnList = "received_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wa_wamid_status", columnNames = {"provider_message_id", "status"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * WhatsApp message identifier (wamid), e.g., "wamid.HBgL..."
     */
    @Column(name = "provider_message_id", length = 150)
    private String providerMessageId;

    /**
     * "STATUS_UPDATE", "INCOMING_MESSAGE", or "UNSUPPORTED"
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * "sent", "delivered", "read", "failed", "received"
     */
    @Column(name = "status", length = 50)
    private String status;

    /**
     * Phone number of customer (from incoming or recipient_id for status)
     */
    @Column(name = "sender_or_recipient", length = 50)
    private String senderOrRecipient;

    /**
     * WhatsApp Business Phone Number ID from metadata
     */
    @Column(name = "phone_number_id", length = 50)
    private String phoneNumberId;

    /**
     * "text", "image", etc. (for incoming messages)
     */
    @Column(name = "message_type", length = 50)
    private String messageType;

    /**
     * Unix timestamp string provided by Meta
     */
    @Column(name = "provider_timestamp", length = 50)
    private String providerTimestamp;

    @Column(name = "error_code")
    private Integer errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = true;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (processed == null) {
            processed = true;
        }
    }
}
