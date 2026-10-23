package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.MessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    Optional<MessageLog> findByProviderMessageId(String providerMessageId);

    Optional<MessageLog> findTopByRecipientPhoneOrderBySentAtDesc(String recipientPhone);

    Page<MessageLog> findAllByOrderBySentAtDesc(Pageable pageable);

    @Query("""
            SELECT m FROM MessageLog m
            WHERE (:channel IS NULL OR m.channel = :channel)
              AND (:messageType IS NULL OR m.messageType = :messageType)
              AND (:status IS NULL OR m.status = :status)
              AND (:search IS NULL OR LOWER(m.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR m.recipientPhone LIKE CONCAT('%', :search, '%')
                   OR STR(m.orderNumber) LIKE CONCAT('%', :search, '%'))
              AND (:startDate IS NULL OR m.sentAt >= :startDate)
              AND (:endDate IS NULL OR m.sentAt <= :endDate)
            ORDER BY m.sentAt DESC
            """)
    Page<MessageLog> filterMessageLogs(
            @Param("channel") String channel,
            @Param("messageType") String messageType,
            @Param("status") String status,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    List<MessageLog> findByOrderIdOrderBySentAtDesc(Long orderId);

    List<MessageLog> findByTrackingUrlContaining(String trackingToken);

    long countByChannel(String channel);

    long countByStatus(String status);

    long countBySentAtAfter(LocalDateTime dateTime);
}
