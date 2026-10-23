package com.sff.authapp.login.controllers;

import com.sff.authapp.login.config.WhatsAppProperties;
import com.sff.authapp.login.security.WhatsAppSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Meta WhatsApp Webhook Security & Integration Tests")
class WhatsAppWebhookSecurityAndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WhatsAppProperties whatsAppProperties;

    @Autowired
    private WhatsAppSignatureValidator signatureValidator;

    private static final String TEST_VERIFY_TOKEN = "test_sff_verify_token_secure_123";
    private static final String TEST_APP_SECRET = "test_sff_meta_app_secret_abc123456789";

    @BeforeEach
    void setupConfig() {
        whatsAppProperties.setVerifyToken(TEST_VERIFY_TOKEN);
        whatsAppProperties.setAppSecret(TEST_APP_SECRET);
    }

    // =========================================================================
    // 1. GET Verification Tests
    // =========================================================================

    @Test
    @DisplayName("GET /api/whatsapp/webhook: Success with valid token and mode returns hub.challenge")
    void testGetVerificationSuccess() throws Exception {
        String challenge = "1158201444";

        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", TEST_VERIFY_TOKEN)
                        .param("hub.challenge", challenge))
                .andExpect(status().isOk())
                .andExpect(content().string(challenge));
    }

    @Test
    @DisplayName("GET /api/whatsapp/webhook: Fails with 403 on incorrect verify token")
    void testGetVerificationIncorrectToken() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong_token_here")
                        .param("hub.challenge", "challenge_123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/whatsapp/webhook: Fails with 403 on incorrect hub.mode")
    void testGetVerificationIncorrectMode() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "invalid_mode")
                        .param("hub.verify_token", TEST_VERIFY_TOKEN)
                        .param("hub.challenge", "challenge_123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/whatsapp/webhook: Fails with 403 when challenge is missing")
    void testGetVerificationMissingChallenge() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", TEST_VERIFY_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/whatsapp/webhook: Fails with 403 when verify_token is missing")
    void testGetVerificationMissingToken() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.challenge", "challenge_123"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. POST Signature Validation & Ingestion Tests
    // =========================================================================

    @Test
    @DisplayName("POST /api/whatsapp/webhook: Success with valid HMAC-SHA256 signature returns 200 OK")
    void testPostValidSignatureAccepted() throws Exception {
        String sampleJson = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "WABA_ID_001",
                      "changes": [
                        {
                          "field": "messages",
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "15550269000",
                              "phone_number_id": "109876543210987"
                            },
                            "statuses": [
                              {
                                "id": "wamid.HBgLtest001",
                                "status": "delivered",
                                "timestamp": "1710000000",
                                "recipient_id": "919876543210"
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        byte[] payloadBytes = sampleJson.getBytes(StandardCharsets.UTF_8);
        String signatureHeader = signatureValidator.generateHeader(payloadBytes, TEST_APP_SECRET);

        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signatureHeader)
                        .content(payloadBytes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EVENT_RECEIVED"));
    }

    @Test
    @DisplayName("POST /api/whatsapp/webhook: Rejects with 401 when signature is invalid")
    void testPostInvalidSignatureRejected() throws Exception {
        String sampleJson = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
        byte[] payloadBytes = sampleJson.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=badbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbad")
                        .content(payloadBytes))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid signature"));
    }

    @Test
    @DisplayName("POST /api/whatsapp/webhook: Rejects with 401 when signature header is missing")
    void testPostMissingSignatureRejected() throws Exception {
        String sampleJson = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";

        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/whatsapp/webhook: Rejects with 401 when request body was tampered")
    void testPostModifiedBodyRejected() throws Exception {
        String originalJson = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
        byte[] originalBytes = originalJson.getBytes(StandardCharsets.UTF_8);
        String header = signatureValidator.generateHeader(originalBytes, TEST_APP_SECRET);

        String tamperedJson = "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"tampered\"}]}";

        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", header)
                        .content(tamperedJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/whatsapp/webhook: Rejects with 400 when body is empty")
    void testPostEmptyBodyRejected() throws Exception {
        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=12345"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/whatsapp/webhook: Rejects with 400 when JSON is malformed despite valid HMAC")
    void testPostMalformedJsonRejected() throws Exception {
        byte[] malformedBytes = "{ invalid json here...".getBytes(StandardCharsets.UTF_8);
        String header = signatureValidator.generateHeader(malformedBytes, TEST_APP_SECRET);

        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", header)
                        .content(malformedBytes))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON"));
    }

    // =========================================================================
    // 3. Security Regression Tests
    // =========================================================================

    @Test
    @DisplayName("Security: Webhook endpoint does NOT require Staff JWT")
    void testWebhookDoesNotRequireJwt() throws Exception {
        // Without Authorization header, GET verification works
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", TEST_VERIFY_TOKEN)
                        .param("hub.challenge", "challenge_ok"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Unrelated protected endpoints STILL require JWT authentication")
    void testUnrelatedProtectedEndpointsStillRequireJwt() throws Exception {
        // Calling staff analytics endpoint without Authorization header must return 401
        mockMvc.perform(get("/api/v1/payments/analytics"))
                .andExpect(status().isUnauthorized());

        // Calling staff communication logs without Authorization header must return 401
        mockMvc.perform(get("/api/v1/communication/logs"))
                .andExpect(status().isUnauthorized());
    }
}
