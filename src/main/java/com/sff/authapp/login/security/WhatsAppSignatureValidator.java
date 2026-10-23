package com.sff.authapp.login.security;

import com.sff.authapp.login.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Validates Meta WhatsApp Cloud API webhook request signatures.
 * <p>
 * Meta uses HMAC-SHA256 computed over the exact raw HTTP request body
 * using the configured Meta App Secret, provided via the 'X-Hub-Signature-256' header.
 * Form: "sha256=<hex_digest>"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppSignatureValidator {

    public static final String SIGNATURE_HEADER_PREFIX = "sha256=";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private final WhatsAppProperties whatsAppProperties;

    /**
     * Validates that the provided raw payload matches the X-Hub-Signature-256 header.
     * Uses constant-time comparison to protect against timing attacks.
     *
     * @param rawPayload      The unmodified raw bytes of the HTTP request body.
     * @param signatureHeader The value of the X-Hub-Signature-256 header.
     * @return true if valid, false otherwise.
     */
    public boolean isValidSignature(byte[] rawPayload, String signatureHeader) {
        if (rawPayload == null || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        String appSecret = whatsAppProperties.getAppSecret();
        if (appSecret == null || appSecret.isBlank()) {
            log.error("WhatsApp signature validation failed: WHATSAPP_APP_SECRET is not configured. Failing securely.");
            return false;
        }

        return validateWithSecret(rawPayload, signatureHeader, appSecret);
    }

    /**
     * Validates a signature against an explicit secret (useful for testing or multi-app setups).
     */
    public boolean validateWithSecret(byte[] rawPayload, String signatureHeader, String appSecret) {
        if (rawPayload == null || signatureHeader == null || signatureHeader.isBlank() || appSecret == null || appSecret.isBlank()) {
            return false;
        }

        String trimmedHeader = signatureHeader.trim();
        if (!trimmedHeader.startsWith(SIGNATURE_HEADER_PREFIX)) {
            log.warn("Invalid signature header format: missing 'sha256=' prefix.");
            return false;
        }

        String providedHash = trimmedHeader.substring(SIGNATURE_HEADER_PREFIX.length()).trim();
        if (providedHash.isBlank()) {
            return false;
        }

        try {
            String expectedHash = calculateHmacSha256(rawPayload, appSecret);

            // Constant-time byte comparison prevents timing analysis vulnerabilities
            byte[] expectedBytes = expectedHash.getBytes(StandardCharsets.UTF_8);
            byte[] providedBytes = providedHash.toLowerCase().getBytes(StandardCharsets.UTF_8);

            return MessageDigest.isEqual(expectedBytes, providedBytes);
        } catch (Exception ex) {
            log.error("Cryptographic error verifying WhatsApp webhook signature: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Computes the HMAC-SHA256 lowercase hex string for the given raw payload and secret.
     */
    public String calculateHmacSha256(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload);
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", ex);
        }
    }

    /**
     * Generates a complete 'sha256=<hex>' header string for a given payload and secret.
     */
    public String generateHeader(byte[] payload, String secret) {
        return SIGNATURE_HEADER_PREFIX + calculateHmacSha256(payload, secret);
    }
}
