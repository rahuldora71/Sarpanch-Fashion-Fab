package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.communication.CommunicationDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface CommunicationLogService {

    Page<MessageLogResponseDto> getMessageLogs(
            String channel,
            String messageType,
            String status,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    Page<OrderAccessLogResponseDto> getAccessLogs(
            String search,
            String deviceType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    Page<AuditLogResponseDto> getAuditLogs(
            String search,
            String action,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    CommunicationStatsDto getStats();

    SendWhatsAppResponseDto sendWhatsAppMessage(SendMessageRequestDto request, String username);

    void recordCustomerAccess(String token, String ipAddress, String userAgent);
}
