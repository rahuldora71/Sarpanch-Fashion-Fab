package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.config.WhatsAppProperties;
import com.sff.authapp.login.dtos.whatsapp.WhatsAppWebhookDtos.*;
import com.sff.authapp.login.entities.MessageLog;
import com.sff.authapp.login.entities.WhatsAppWebhookEvent;
import com.sff.authapp.login.repositories.MessageLogRepository;
import com.sff.authapp.login.repositories.WhatsAppWebhookEventRepository;
import com.sff.authapp.login.services.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookServiceImpl implements WhatsAppWebhookService {

    private final WhatsAppProperties whatsAppProperties;
    private final WhatsAppWebhookEventRepository eventRepository;
    private final MessageLogRepository messageLogRepository;

    @Override
    public boolean verifyWebhookToken(String mode, String verifyToken) {
        if (mode == null || !mode.equals("subscribe")) {
            return false;
        }

        if (verifyToken == null || verifyToken.isBlank()) {
            return false;
        }

        String configuredToken = whatsAppProperties.getVerifyToken();
        if (configuredToken == null || configuredToken.isBlank()) {
            log.error("WhatsApp webhook GET verification failed: WHATSAPP_VERIFY_TOKEN is not configured in backend.");
            return false;
        }

        // Constant-time byte comparison prevents timing analysis vulnerabilities
        byte[] expectedBytes = configuredToken.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = verifyToken.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }

    @Override
    @Transactional
    public void processWebhookPayload(WhatsAppWebhookPayloadDto payload, byte[] rawPayload) {
        if (payload == null) {
            log.warn("Received empty or null WhatsApp webhook payload");
            return;
        }

        // Verify top-level object is "whatsapp_business_account"
        if (payload.getObject() != null && !payload.getObject().equals("whatsapp_business_account")) {
            log.info("Ignored unsupported Meta webhook object type: {}", payload.getObject());
            return;
        }

        List<WhatsAppEntryDto> entries = payload.getEntry();
        if (entries == null || entries.isEmpty()) {
            log.debug("WhatsApp webhook payload has no entries");
            return;
        }

        String configuredPhoneId = whatsAppProperties.getPhoneNumberId();

        for (WhatsAppEntryDto entry : entries) {
            if (entry.getChanges() == null) continue;

            for (WhatsAppChangeDto change : entry.getChanges()) {
                if (change == null) continue;

                // Meta WhatsApp Webhook field for messages and statuses is "messages"
                if (!"messages".equals(change.getField())) {
                    log.debug("Ignored non-messages webhook field: {}", change.getField());
                    continue;
                }

                WhatsAppValueDto value = change.getValue();
                if (value == null) continue;

                // Check Phone Number ID if configured
                String eventPhoneId = value.getMetadata() != null ? value.getMetadata().getPhoneNumberId() : null;
                if (configuredPhoneId != null && !configuredPhoneId.isBlank()
                        && eventPhoneId != null && !configuredPhoneId.equals(eventPhoneId)) {
                    log.info("Received WhatsApp event for non-configured phone number ID (received: {}, configured: {})",
                            maskId(eventPhoneId), maskId(configuredPhoneId));
                }

                // 1. Process Message Status Updates (sent, delivered, read, failed)
                processStatuses(value.getStatuses(), eventPhoneId);

                // 2. Process Incoming Customer Messages (text, interactive, media, etc.)
                processMessages(value.getMessages(), eventPhoneId);
            }
        }
    }

    private void processStatuses(List<WhatsAppMessageStatusDto> statuses, String phoneNumberId) {
        if (statuses == null || statuses.isEmpty()) return;

        for (WhatsAppMessageStatusDto statusDto : statuses) {
            if (statusDto == null) continue;

            String wamid = statusDto.getId();
            String status = statusDto.getStatus() != null ? statusDto.getStatus().toLowerCase() : "unknown";
            String recipientId = statusDto.getRecipientId();
            String timestamp = statusDto.getTimestamp();

            // Extract error information if present (e.g. status == "failed")
            Integer errorCode = null;
            String errorMessage = null;
            if (statusDto.getErrors() != null && !statusDto.getErrors().isEmpty()) {
                WhatsAppErrorDto err = statusDto.getErrors().get(0);
                errorCode = err.getCode();
                errorMessage = err.getTitle() != null ? err.getTitle() : err.getMessage();
            }

            // Idempotency: Skip duplicate delivery of the exact same status for this message ID
            if (wamid != null && eventRepository.existsByProviderMessageIdAndStatus(wamid, status)) {
                log.info("Duplicate webhook event ignored (status '{}' already recorded for wamid: {})",
                        status, maskId(wamid));
                continue;
            }

            // Persist durable audit log of the event
            WhatsAppWebhookEvent event = WhatsAppWebhookEvent.builder()
                    .providerMessageId(wamid)
                    .eventType("STATUS_UPDATE")
                    .status(status)
                    .senderOrRecipient(recipientId)
                    .phoneNumberId(phoneNumberId)
                    .providerTimestamp(timestamp)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .processed(true)
                    .receivedAt(LocalDateTime.now())
                    .build();

            try {
                eventRepository.save(event);
            } catch (DataIntegrityViolationException ex) {
                log.info("Concurrent duplicate status update ignored for wamid: {}, status: {}", maskId(wamid), status);
                continue;
            }

            // Update matching MessageLog if present, observing monotonic status hierarchy
            updateMessageLog(wamid, status, recipientId);
        }
    }

    private void processMessages(List<WhatsAppIncomingMessageDto> messages, String phoneNumberId) {
        if (messages == null || messages.isEmpty()) return;

        for (WhatsAppIncomingMessageDto msgDto : messages) {
            if (msgDto == null) continue;

            String wamid = msgDto.getId();
            String from = msgDto.getFrom();
            String type = msgDto.getType() != null ? msgDto.getType() : "text";
            String timestamp = msgDto.getTimestamp();

            // Idempotency: Skip duplicate delivery of this incoming message ID
            if (wamid != null && eventRepository.existsByProviderMessageId(wamid)) {
                log.info("Duplicate incoming message ignored for wamid: {}", maskId(wamid));
                continue;
            }

            WhatsAppWebhookEvent event = WhatsAppWebhookEvent.builder()
                    .providerMessageId(wamid)
                    .eventType("INCOMING_MESSAGE")
                    .status("received")
                    .senderOrRecipient(from)
                    .phoneNumberId(phoneNumberId)
                    .messageType(type)
                    .providerTimestamp(timestamp)
                    .processed(true)
                    .receivedAt(LocalDateTime.now())
                    .build();

            try {
                eventRepository.save(event);
            } catch (DataIntegrityViolationException ex) {
                log.info("Concurrent duplicate incoming message ignored for wamid: {}", maskId(wamid));
                continue;
            }

            // Log reception without logging sensitive message body or full customer details
            log.info("Accepted incoming WhatsApp customer message [type: {}] (wamid: {})", type, maskId(wamid));
        }
    }

    private void updateMessageLog(String wamid, String newStatus, String recipientPhone) {
        Optional<MessageLog> messageLogOpt = Optional.empty();

        if (wamid != null && !wamid.isBlank()) {
            messageLogOpt = messageLogRepository.findByProviderMessageId(wamid);
        }

        if (messageLogOpt.isEmpty() && recipientPhone != null && !recipientPhone.isBlank()) {
            messageLogOpt = messageLogRepository.findTopByRecipientPhoneOrderBySentAtDesc(recipientPhone);
        }

        if (messageLogOpt.isEmpty()) {
            return;
        }

        MessageLog msg = messageLogOpt.get();
        if (wamid != null && (msg.getProviderMessageId() == null || msg.getProviderMessageId().isBlank())) {
            msg.setProviderMessageId(wamid);
        }

        String current = msg.getStatus() != null ? msg.getStatus().toUpperCase() : "SENT";
        String target = newStatus != null ? newStatus.toUpperCase() : "";

        // Status rank: SENT (1) < DELIVERED (2) < READ (3) / CLICKED (3)
        // FAILED is terminal error state
        int currentRank = getStatusRank(current);
        int targetRank = getStatusRank(target);

        if (target.equals("FAILED")) {
            msg.setStatus("FAILED");
            messageLogRepository.save(msg);
            return;
        }

        // Never downgrade a higher status if out-of-order notification arrives
        if (targetRank > currentRank) {
            msg.setStatus(target);
            if ("READ".equals(target) && msg.getClickedAt() == null) {
                msg.setClickedAt(LocalDateTime.now());
            }
            messageLogRepository.save(msg);
            log.debug("Updated MessageLog ID {} status from '{}' to '{}'", msg.getId(), current, target);
        } else {
            log.debug("Preserved higher status '{}' over incoming out-of-order '{}' for MessageLog ID {}",
                    current, target, msg.getId());
        }
    }

    private int getStatusRank(String status) {
        if (status == null) return 0;
        return switch (status.toUpperCase()) {
            case "SENT" -> 1;
            case "DELIVERED" -> 2;
            case "READ", "CLICKED" -> 3;
            case "FAILED" -> -1;
            default -> 0;
        };
    }

    private String maskId(String id) {
        if (id == null) return "null";
        if (id.length() <= 8) return "***";
        return id.substring(0, Math.min(6, id.length())) + "..." + id.substring(id.length() - 4);
    }
}
