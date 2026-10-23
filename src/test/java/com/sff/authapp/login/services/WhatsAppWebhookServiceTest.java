package com.sff.authapp.login.services;

import com.sff.authapp.login.config.WhatsAppProperties;
import com.sff.authapp.login.dtos.whatsapp.WhatsAppWebhookDtos.*;
import com.sff.authapp.login.entities.MessageLog;
import com.sff.authapp.login.entities.WhatsAppWebhookEvent;
import com.sff.authapp.login.repositories.MessageLogRepository;
import com.sff.authapp.login.repositories.WhatsAppWebhookEventRepository;
import com.sff.authapp.login.services.impl.WhatsAppWebhookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WhatsApp Webhook Service Unit Tests")
class WhatsAppWebhookServiceTest {

    @Mock
    private WhatsAppWebhookEventRepository eventRepository;

    @Mock
    private MessageLogRepository messageLogRepository;

    private WhatsAppProperties properties;
    private WhatsAppWebhookServiceImpl service;

    private static final String VERIFY_TOKEN = "my_secure_sff_verify_token_2026";
    private static final String PHONE_NUMBER_ID = "109876543210987";

    @BeforeEach
    void setUp() {
        properties = new WhatsAppProperties();
        properties.setVerifyToken(VERIFY_TOKEN);
        properties.setPhoneNumberId(PHONE_NUMBER_ID);

        service = new WhatsAppWebhookServiceImpl(properties, eventRepository, messageLogRepository);
    }

    // =========================================================================
    // GET Verification Tests
    // =========================================================================

    @Test
    @DisplayName("GET Verification: Valid mode and token should return true")
    void testVerifyWebhookTokenSuccess() {
        assertTrue(service.verifyWebhookToken("subscribe", VERIFY_TOKEN));
    }

    @Test
    @DisplayName("GET Verification: Incorrect verification token should return false")
    void testVerifyWebhookTokenMismatch() {
        assertFalse(service.verifyWebhookToken("subscribe", "wrong_token_value"));
    }

    @Test
    @DisplayName("GET Verification: Incorrect mode should return false")
    void testVerifyWebhookModeMismatch() {
        assertFalse(service.verifyWebhookToken("unsubscribe", VERIFY_TOKEN));
        assertFalse(service.verifyWebhookToken(null, VERIFY_TOKEN));
    }

    @Test
    @DisplayName("GET Verification: Missing or blank token should return false")
    void testVerifyWebhookTokenMissing() {
        assertFalse(service.verifyWebhookToken("subscribe", null));
        assertFalse(service.verifyWebhookToken("subscribe", ""));
        assertFalse(service.verifyWebhookToken("subscribe", "   "));
    }

    @Test
    @DisplayName("GET Verification: Empty configured token should fail safely")
    void testVerifyWebhookEmptyConfiguredToken() {
        properties.setVerifyToken("");
        assertFalse(service.verifyWebhookToken("subscribe", VERIFY_TOKEN));

        properties.setVerifyToken(null);
        assertFalse(service.verifyWebhookToken("subscribe", VERIFY_TOKEN));
    }

    // =========================================================================
    // POST Event Processing Tests
    // =========================================================================

    @Test
    @DisplayName("POST Processing: Incoming message event is persisted idempotently")
    void testProcessIncomingMessage() {
        String wamid = "wamid.HBgLMTE5ODc2NTQzMjEwFQIAEhgWM0VCMDFEMUY1ODk3QzhCNzYyNDEyNwA=";
        when(eventRepository.existsByProviderMessageId(wamid)).thenReturn(false);

        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithMessages(wamid, "919876543210", "text", "Hello Tailor");

        service.processWebhookPayload(payload, new byte[0]);

        ArgumentCaptor<WhatsAppWebhookEvent> captor = ArgumentCaptor.forClass(WhatsAppWebhookEvent.class);
        verify(eventRepository).save(captor.capture());

        WhatsAppWebhookEvent savedEvent = captor.getValue();
        assertEquals(wamid, savedEvent.getProviderMessageId());
        assertEquals("INCOMING_MESSAGE", savedEvent.getEventType());
        assertEquals("received", savedEvent.getStatus());
        assertEquals("919876543210", savedEvent.getSenderOrRecipient());
        assertEquals("text", savedEvent.getMessageType());
    }

    @Test
    @DisplayName("POST Processing: Sent status update records event and updates MessageLog")
    void testProcessSentStatus() {
        String wamid = "wamid.HBgLsent001";
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid, "sent")).thenReturn(false);

        MessageLog existingMsg = MessageLog.builder()
                .id(10L)
                .providerMessageId(wamid)
                .status("SENT")
                .build();
        when(messageLogRepository.findByProviderMessageId(wamid)).thenReturn(Optional.of(existingMsg));

        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithStatus(wamid, "sent", "919876543210", null, null);

        service.processWebhookPayload(payload, new byte[0]);

        verify(eventRepository).save(any(WhatsAppWebhookEvent.class));
    }

    @Test
    @DisplayName("POST Processing: Delivered status updates MessageLog from SENT to DELIVERED")
    void testProcessDeliveredStatus() {
        String wamid = "wamid.HBgLdelivered002";
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid, "delivered")).thenReturn(false);

        MessageLog existingMsg = MessageLog.builder()
                .id(11L)
                .providerMessageId(wamid)
                .status("SENT")
                .build();
        when(messageLogRepository.findByProviderMessageId(wamid)).thenReturn(Optional.of(existingMsg));

        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithStatus(wamid, "delivered", "919876543210", null, null);

        service.processWebhookPayload(payload, new byte[0]);

        assertEquals("DELIVERED", existingMsg.getStatus());
        verify(messageLogRepository).save(existingMsg);
    }

    @Test
    @DisplayName("POST Processing: Read status updates MessageLog from DELIVERED to READ")
    void testProcessReadStatus() {
        String wamid = "wamid.HBgLread003";
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid, "read")).thenReturn(false);

        MessageLog existingMsg = MessageLog.builder()
                .id(12L)
                .providerMessageId(wamid)
                .status("DELIVERED")
                .build();
        when(messageLogRepository.findByProviderMessageId(wamid)).thenReturn(Optional.of(existingMsg));

        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithStatus(wamid, "read", "919876543210", null, null);

        service.processWebhookPayload(payload, new byte[0]);

        assertEquals("READ", existingMsg.getStatus());
        assertNotNull(existingMsg.getClickedAt());
        verify(messageLogRepository).save(existingMsg);
    }

    @Test
    @DisplayName("POST Processing: Out-of-order delivered update does NOT downgrade a READ message")
    void testOutOfOrderProtection() {
        String wamid = "wamid.HBgLoutoforder004";
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid, "delivered")).thenReturn(false);

        // Message is already READ
        MessageLog existingMsg = MessageLog.builder()
                .id(13L)
                .providerMessageId(wamid)
                .status("READ")
                .build();
        when(messageLogRepository.findByProviderMessageId(wamid)).thenReturn(Optional.of(existingMsg));

        // Out of order: Meta sends "delivered" after "read" was already processed
        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithStatus(wamid, "delivered", "919876543210", null, null);

        service.processWebhookPayload(payload, new byte[0]);

        // Status MUST NOT be downgraded from READ to DELIVERED
        assertEquals("READ", existingMsg.getStatus());
        verify(eventRepository).save(any(WhatsAppWebhookEvent.class));
        verify(messageLogRepository, never()).save(existingMsg);
    }

    @Test
    @DisplayName("POST Processing: Failed status records error code and title")
    void testProcessFailedStatus() {
        String wamid = "wamid.HBgLfailed005";
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid, "failed")).thenReturn(false);

        MessageLog existingMsg = MessageLog.builder()
                .id(14L)
                .providerMessageId(wamid)
                .status("SENT")
                .build();
        when(messageLogRepository.findByProviderMessageId(wamid)).thenReturn(Optional.of(existingMsg));

        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithStatus(wamid, "failed", "919876543210", 131051, "Unsupported message type");

        service.processWebhookPayload(payload, new byte[0]);

        assertEquals("FAILED", existingMsg.getStatus());
        verify(messageLogRepository).save(existingMsg);

        ArgumentCaptor<WhatsAppWebhookEvent> captor = ArgumentCaptor.forClass(WhatsAppWebhookEvent.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(131051, captor.getValue().getErrorCode());
        assertEquals("Unsupported message type", captor.getValue().getErrorMessage());
    }

    @Test
    @DisplayName("POST Processing: Duplicate event delivery is skipped (Idempotency)")
    void testDuplicateDeliverySkipped() {
        String wamid = "wamid.HBgLduplicate006";
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid, "delivered")).thenReturn(true);

        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithStatus(wamid, "delivered", "919876543210", null, null);

        service.processWebhookPayload(payload, new byte[0]);

        verify(eventRepository, never()).save(any(WhatsAppWebhookEvent.class));
        verify(messageLogRepository, never()).save(any(MessageLog.class));
    }

    @Test
    @DisplayName("POST Processing: Concurrent duplicate delivery caught via DataIntegrityViolationException")
    void testConcurrentDuplicateDelivery() {
        String wamid = "wamid.HBgLconcurrent007";
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid, "delivered")).thenReturn(false);
        when(eventRepository.save(any(WhatsAppWebhookEvent.class))).thenThrow(new DataIntegrityViolationException("Duplicate key uk_wa_wamid_status"));

        WhatsAppWebhookPayloadDto payload = createSamplePayloadWithStatus(wamid, "delivered", "919876543210", null, null);

        // Does not throw unhandled exception
        assertDoesNotThrow(() -> service.processWebhookPayload(payload, new byte[0]));
    }

    @Test
    @DisplayName("POST Processing: Unknown object type or non-messages field is safely ignored")
    void testUnknownObjectTypeAndFields() {
        WhatsAppWebhookPayloadDto payload = WhatsAppWebhookPayloadDto.builder()
                .object("user_account") // Non-WABA object
                .build();

        assertDoesNotThrow(() -> service.processWebhookPayload(payload, new byte[0]));
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("POST Processing: Missing optional fields does not cause NullPointerException")
    void testMissingOptionalFields() {
        WhatsAppWebhookPayloadDto payload = WhatsAppWebhookPayloadDto.builder()
                .object("whatsapp_business_account")
                .entry(List.of(
                        WhatsAppEntryDto.builder()
                                .changes(List.of(
                                        WhatsAppChangeDto.builder()
                                                .field("messages")
                                                .value(WhatsAppValueDto.builder()
                                                        .metadata(null)
                                                        .contacts(null)
                                                        .messages(null)
                                                        .statuses(null)
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        assertDoesNotThrow(() -> service.processWebhookPayload(payload, new byte[0]));
    }

    @Test
    @DisplayName("POST Processing: Multiple entries and changes in a single payload are all processed")
    void testMultipleEntriesAndChanges() {
        String wamid1 = "wamid.HBgLmulti001";
        String wamid2 = "wamid.HBgLmulti002";

        when(eventRepository.existsByProviderMessageIdAndStatus(wamid1, "delivered")).thenReturn(false);
        when(eventRepository.existsByProviderMessageIdAndStatus(wamid2, "read")).thenReturn(false);

        WhatsAppWebhookPayloadDto payload = WhatsAppWebhookPayloadDto.builder()
                .object("whatsapp_business_account")
                .entry(List.of(
                        WhatsAppEntryDto.builder()
                                .changes(List.of(
                                        WhatsAppChangeDto.builder()
                                                .field("messages")
                                                .value(WhatsAppValueDto.builder()
                                                        .statuses(List.of(
                                                                WhatsAppMessageStatusDto.builder()
                                                                        .id(wamid1)
                                                                        .status("delivered")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build(),
                                        WhatsAppChangeDto.builder()
                                                .field("messages")
                                                .value(WhatsAppValueDto.builder()
                                                        .statuses(List.of(
                                                                WhatsAppMessageStatusDto.builder()
                                                                        .id(wamid2)
                                                                        .status("read")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        service.processWebhookPayload(payload, new byte[0]);

        verify(eventRepository, times(2)).save(any(WhatsAppWebhookEvent.class));
    }

    // =========================================================================
    // Fixture Helpers
    // =========================================================================

    private WhatsAppWebhookPayloadDto createSamplePayloadWithMessages(String wamid, String from, String type, String body) {
        return WhatsAppWebhookPayloadDto.builder()
                .object("whatsapp_business_account")
                .entry(List.of(
                        WhatsAppEntryDto.builder()
                                .id("WABA_ID_001")
                                .changes(List.of(
                                        WhatsAppChangeDto.builder()
                                                .field("messages")
                                                .value(WhatsAppValueDto.builder()
                                                        .messagingProduct("whatsapp")
                                                        .metadata(WhatsAppMetadataDto.builder()
                                                                .displayPhoneNumber("15550269000")
                                                                .phoneNumberId(PHONE_NUMBER_ID)
                                                                .build())
                                                        .contacts(List.of(
                                                                WhatsAppContactDto.builder()
                                                                        .waId(from)
                                                                        .profile(WhatsAppProfileDto.builder().name("Customer").build())
                                                                        .build()
                                                        ))
                                                        .messages(List.of(
                                                                WhatsAppIncomingMessageDto.builder()
                                                                        .id(wamid)
                                                                        .from(from)
                                                                        .timestamp("1710000000")
                                                                        .type(type)
                                                                        .text(WhatsAppTextContentDto.builder().body(body).build())
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    private WhatsAppWebhookPayloadDto createSamplePayloadWithStatus(String wamid, String status, String recipient, Integer errCode, String errMsg) {
        List<WhatsAppErrorDto> errors = (errCode != null)
                ? List.of(WhatsAppErrorDto.builder().code(errCode).title(errMsg).message(errMsg).build())
                : Collections.emptyList();

        return WhatsAppWebhookPayloadDto.builder()
                .object("whatsapp_business_account")
                .entry(List.of(
                        WhatsAppEntryDto.builder()
                                .id("WABA_ID_001")
                                .changes(List.of(
                                        WhatsAppChangeDto.builder()
                                                .field("messages")
                                                .value(WhatsAppValueDto.builder()
                                                        .messagingProduct("whatsapp")
                                                        .metadata(WhatsAppMetadataDto.builder()
                                                                .displayPhoneNumber("15550269000")
                                                                .phoneNumberId(PHONE_NUMBER_ID)
                                                                .build())
                                                        .statuses(List.of(
                                                                WhatsAppMessageStatusDto.builder()
                                                                        .id(wamid)
                                                                        .status(status)
                                                                        .recipientId(recipient)
                                                                        .timestamp("1710000000")
                                                                        .conversation(Map.of("id", "CONV_001"))
                                                                        .pricing(Map.of("billable", true))
                                                                        .errors(errors)
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }
}
