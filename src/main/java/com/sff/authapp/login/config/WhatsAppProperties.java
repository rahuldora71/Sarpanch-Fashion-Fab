package com.sff.authapp.login.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Meta WhatsApp Business Platform / Cloud API.
 * Values are injected from application.properties or environment variables:
 * - WHATSAPP_VERIFY_TOKEN
 * - WHATSAPP_APP_SECRET
 * - WHATSAPP_ACCESS_TOKEN
 * - WHATSAPP_PHONE_NUMBER_ID
 * - WHATSAPP_API_VERSION
 */
@Configuration
@ConfigurationProperties(prefix = "whatsapp")
@Getter
@Setter
public class WhatsAppProperties {

    /**
     * Webhook verification token shared with Meta App Dashboard (hub.verify_token).
     * Backed by WHATSAPP_VERIFY_TOKEN.
     */
    private String verifyToken = "";

    /**
     * Meta App Secret used to validate HMAC-SHA256 signature in X-Hub-Signature-256 header.
     * Backed by WHATSAPP_APP_SECRET. Never expose in frontend or client responses.
     */
    private String appSecret = "";

    /**
     * WhatsApp Cloud API System User / Permanent Access Token for outbound requests.
     * Backed by WHATSAPP_ACCESS_TOKEN. Never expose in logs or frontend.
     */
    private String accessToken = "";

    /**
     * WhatsApp Phone Number ID assigned in Meta App Dashboard.
     * Backed by WHATSAPP_PHONE_NUMBER_ID.
     */
    private String phoneNumberId = "";

    /**
     * Meta Graph API version (e.g. "v21.0").
     * Backed by WHATSAPP_API_VERSION.
     */
    private String apiVersion = "v21.0";
}
