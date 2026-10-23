package com.sff.authapp.login.dtos.communication;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class CommunicationDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageLogResponseDto {
        private Long id;
        private Long orderId;
        private Long orderNumber;
        private Long customerId;
        private String customerName;
        private String recipientPhone;
        private String channel;
        private String messageType;
        private String messageContent;
        private String trackingUrl;
        private String status;
        private String sentBy;
        private LocalDateTime sentAt;
        private LocalDateTime clickedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderAccessLogResponseDto {
        private Long id;
        private Long orderId;
        private Long orderNumber;
        private String trackingToken;
        private String customerName;
        private String ipAddress;
        private String userAgent;
        private String deviceType;
        private LocalDateTime accessedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLogResponseDto {
        private Long id;
        private Long userId;
        private String userName;
        private String action;
        private String entityName;
        private Long entityId;
        private String details;
        private String ipAddress;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommunicationStatsDto {
        private long totalMessages;
        private long whatsappCount;
        private long smsCount;
        private long deliveredCount;
        private long clickedCount;
        private double clickThroughRate;
        private long todayMessages;
        private long todayAccesses;
        private long totalCustomerAccesses;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendMessageRequestDto {
        private Long orderId;
        private Long customerId;
        private String customerName;
        @NotBlank(message = "Phone number is required")
        private String phone;
        @Builder.Default
        private String channel = "WHATSAPP"; // "WHATSAPP" or "SMS"
        private String messageType; // "ORDER_CONFIRMATION", "READY_FOR_TRIAL", "DELIVERED", etc.
        private String customMessage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendWhatsAppResponseDto {
        private Long logId;
        private String whatsappUrl;
        private String recipientPhone;
        private String messageContent;
        private String trackingUrl;
        private String status;
    }
}
