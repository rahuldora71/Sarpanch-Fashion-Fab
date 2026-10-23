package com.sff.authapp.login.dtos.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects for official Meta WhatsApp Cloud API Webhook payloads.
 * Based on Meta's WhatsApp Business Platform Webhook reference.
 * All DTOs ignore unknown properties to remain resilient to API additions.
 */
public final class WhatsAppWebhookDtos {

    private WhatsAppWebhookDtos() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppWebhookPayloadDto {

        /**
         * Usually "whatsapp_business_account"
         */
        @JsonProperty("object")
        private String object;

        @JsonProperty("entry")
        private List<WhatsAppEntryDto> entry;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppEntryDto {

        /**
         * WhatsApp Business Account ID (WABA ID)
         */
        @JsonProperty("id")
        private String id;

        @JsonProperty("changes")
        private List<WhatsAppChangeDto> changes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppChangeDto {

        /**
         * Webhook field subscribed to, e.g., "messages"
         */
        @JsonProperty("field")
        private String field;

        @JsonProperty("value")
        private WhatsAppValueDto value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppValueDto {

        /**
         * Always "whatsapp"
         */
        @JsonProperty("messaging_product")
        private String messagingProduct;

        @JsonProperty("metadata")
        private WhatsAppMetadataDto metadata;

        @JsonProperty("contacts")
        private List<WhatsAppContactDto> contacts;

        @JsonProperty("messages")
        private List<WhatsAppIncomingMessageDto> messages;

        @JsonProperty("statuses")
        private List<WhatsAppMessageStatusDto> statuses;

        @JsonProperty("errors")
        private List<WhatsAppErrorDto> errors;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppMetadataDto {

        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;

        @JsonProperty("phone_number_id")
        private String phoneNumberId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppContactDto {

        @JsonProperty("wa_id")
        private String waId;

        @JsonProperty("profile")
        private WhatsAppProfileDto profile;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppProfileDto {

        @JsonProperty("name")
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppIncomingMessageDto {

        /**
         * Unique WhatsApp message identifier (e.g., "wamid.HBgL...")
         */
        @JsonProperty("id")
        private String id;

        /**
         * Customer sender phone number in international format without '+'
         */
        @JsonProperty("from")
        private String from;

        /**
         * Unix timestamp string from Meta
         */
        @JsonProperty("timestamp")
        private String timestamp;

        /**
         * "text", "image", "document", "audio", "video", "button", "interactive", "reaction", etc.
         */
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private WhatsAppTextContentDto text;

        @JsonProperty("image")
        private WhatsAppMediaContentDto image;

        @JsonProperty("errors")
        private List<WhatsAppErrorDto> errors;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppTextContentDto {

        @JsonProperty("body")
        private String body;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppMediaContentDto {

        @JsonProperty("id")
        private String id;

        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("sha256")
        private String sha256;

        @JsonProperty("caption")
        private String caption;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppMessageStatusDto {

        /**
         * WhatsApp message ID being tracked (wamid)
         */
        @JsonProperty("id")
        private String id;

        /**
         * "sent", "delivered", "read", "failed"
         */
        @JsonProperty("status")
        private String status;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("recipient_id")
        private String recipientId;

        @JsonProperty("conversation")
        private Map<String, Object> conversation;

        @JsonProperty("pricing")
        private Map<String, Object> pricing;

        @JsonProperty("errors")
        private List<WhatsAppErrorDto> errors;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppErrorDto {

        @JsonProperty("code")
        private Integer code;

        @JsonProperty("title")
        private String title;

        @JsonProperty("message")
        private String message;

        @JsonProperty("error_data")
        private Map<String, Object> errorData;
    }
}
