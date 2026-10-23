package com.sff.authapp.login.security;

import com.sff.authapp.login.config.WhatsAppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WhatsApp Signature Validator Unit Tests")
class WhatsAppSignatureValidatorTest {

    private WhatsAppProperties properties;
    private WhatsAppSignatureValidator validator;

    private static final String APP_SECRET = "test_meta_app_secret_1234567890abcdef";
    private static final String RAW_PAYLOAD = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";

    @BeforeEach
    void setUp() {
        properties = new WhatsAppProperties();
        properties.setAppSecret(APP_SECRET);
        validator = new WhatsAppSignatureValidator(properties);
    }

    @Test
    @DisplayName("Should accept valid HMAC-SHA256 signature with sha256= prefix")
    void testValidSignature() {
        byte[] payloadBytes = RAW_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String header = validator.generateHeader(payloadBytes, APP_SECRET);

        assertTrue(validator.isValidSignature(payloadBytes, header));
    }

    @Test
    @DisplayName("Should reject incorrect signature hash")
    void testIncorrectSignature() {
        byte[] payloadBytes = RAW_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String wrongHeader = "sha256=0000000000000000000000000000000000000000000000000000000000000000";

        assertFalse(validator.isValidSignature(payloadBytes, wrongHeader));
    }

    @Test
    @DisplayName("Should reject missing signature header")
    void testMissingSignatureHeader() {
        byte[] payloadBytes = RAW_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        assertFalse(validator.isValidSignature(payloadBytes, null));
        assertFalse(validator.isValidSignature(payloadBytes, ""));
        assertFalse(validator.isValidSignature(payloadBytes, "   "));
    }

    @Test
    @DisplayName("Should reject malformed signature header lacking sha256= prefix")
    void testMalformedSignatureHeader() {
        byte[] payloadBytes = RAW_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String rawHash = validator.calculateHmacSha256(payloadBytes, APP_SECRET);

        // Missing "sha256=" prefix
        assertFalse(validator.isValidSignature(payloadBytes, rawHash));

        // Wrong prefix
        assertFalse(validator.isValidSignature(payloadBytes, "sha1=" + rawHash));
        assertFalse(validator.isValidSignature(payloadBytes, "md5=" + rawHash));

        // Only prefix without hash
        assertFalse(validator.isValidSignature(payloadBytes, "sha256="));
    }

    @Test
    @DisplayName("Should reject signature when request body was modified after signature generation")
    void testModifiedRequestBody() {
        byte[] originalPayload = RAW_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String header = validator.generateHeader(originalPayload, APP_SECRET);

        // Tamper with one character in payload
        byte[] tamperedPayload = "{\"object\":\"whatsapp_business_account\",\"entry\":[ ]}".getBytes(StandardCharsets.UTF_8);

        assertFalse(validator.isValidSignature(tamperedPayload, header));
    }

    @Test
    @DisplayName("Should fail securely when App Secret is missing or blank")
    void testMissingAppSecret() {
        properties.setAppSecret("");
        byte[] payloadBytes = RAW_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String header = "sha256=1234567890abcdef";

        assertFalse(validator.isValidSignature(payloadBytes, header));

        properties.setAppSecret(null);
        assertFalse(validator.isValidSignature(payloadBytes, header));
    }

    @Test
    @DisplayName("Should reject empty request body")
    void testEmptyRequestBody() {
        String header = validator.generateHeader(new byte[0], APP_SECRET);

        assertFalse(validator.isValidSignature(null, header));
    }
}
