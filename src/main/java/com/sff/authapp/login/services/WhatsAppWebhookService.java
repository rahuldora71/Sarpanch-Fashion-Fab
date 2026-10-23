package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.whatsapp.WhatsAppWebhookDtos.WhatsAppWebhookPayloadDto;

/**
 * Service handling Meta WhatsApp Cloud API webhook verification,
 * event ingestion, idempotency, and status updates.
 */
public interface WhatsAppWebhookService {

    /**
     * Verifies the hub.mode and hub.verify_token sent by Meta during webhook subscription setup.
     *
     * @param mode        The hub.mode query parameter (must be "subscribe")
     * @param verifyToken The hub.verify_token query parameter to compare against configured token
     * @return true if valid, false otherwise
     */
    boolean verifyWebhookToken(String mode, String verifyToken);

    /**
     * Ingests, validates, and idempotently processes the WhatsApp Cloud API webhook payload.
     *
     * @param payload    The deserialized webhook payload
     * @param rawPayload The raw bytes of the request body
     */
    void processWebhookPayload(WhatsAppWebhookPayloadDto payload, byte[] rawPayload);
}
