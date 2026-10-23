package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.WhatsAppWebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppWebhookEventRepository extends JpaRepository<WhatsAppWebhookEvent, Long> {

    /**
     * Checks if a status event for this message ID (wamid) and status has already been recorded.
     * Ensures idempotent status updates upon Meta webhook retries.
     */
    boolean existsByProviderMessageIdAndStatus(String providerMessageId, String status);

    /**
     * Checks if an incoming message event with this message ID (wamid) has already been recorded.
     */
    boolean existsByProviderMessageId(String providerMessageId);

    Optional<WhatsAppWebhookEvent> findByProviderMessageIdAndStatus(String providerMessageId, String status);

    List<WhatsAppWebhookEvent> findByProviderMessageIdOrderByReceivedAtDesc(String providerMessageId);

    Page<WhatsAppWebhookEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
