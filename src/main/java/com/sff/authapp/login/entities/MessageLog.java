package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "message_logs",
        indexes = {
                @Index(name = "idx_msg_order", columnList = "order_id"),
                @Index(name = "idx_msg_customer", columnList = "customer_name"),
                @Index(name = "idx_msg_phone", columnList = "recipient_phone"),
                @Index(name = "idx_msg_channel", columnList = "channel"),
                @Index(name = "idx_msg_provider_id", columnList = "provider_message_id"),
                @Index(name = "idx_msg_created", columnList = "sent_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_number")
    private Long orderNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;

    /**
     * Provider Message ID (Meta WhatsApp wamid, e.g. "wamid.HBgL...")
     */
    @Column(name = "provider_message_id", length = 150)
    private String providerMessageId;

    /**
     * "WHATSAPP" or "SMS"
     */
    @Column(name = "channel", length = 30)
    private String channel;

    /**
     * "ORDER_CONFIRMATION", "PRODUCTION_UPDATE", "READY_FOR_TRIAL",
     * "DELIVERED", "INVOICE_BILL", "REVIEW_REQUEST", "CUSTOM"
     */
    @Column(name = "message_type", length = 50)
    private String messageType;

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    /**
     * "SENT", "DELIVERED", "CLICKED", "FAILED"
     */
    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "SENT";

    @Column(name = "sent_by", length = 100)
    private String sentBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "SENT";
        }
        if (channel == null || channel.isBlank()) {
            channel = "WHATSAPP";
        }
        if (messageType == null || messageType.isBlank()) {
            messageType = "CUSTOM";
        }
        if (messageContent == null) {
            messageContent = "";
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            recipientPhone = "N/A";
        }
    }
}
