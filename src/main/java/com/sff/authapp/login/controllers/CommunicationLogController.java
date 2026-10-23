package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.communication.CommunicationDtos.*;
import com.sff.authapp.login.services.CommunicationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/communication")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Communication & Access Logs",
        description = "APIs for tracking sent WhatsApp/SMS messages, customer link clicks, and access audit logs"
)
public class CommunicationLogController {

    private final CommunicationLogService communicationLogService;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get paginated message logs (WhatsApp & SMS)")
    public ResponseEntity<Page<MessageLogResponseDto>> getMessageLogs(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                communicationLogService.getMessageLogs(channel, messageType, status, search, startDate, endDate, pageable)
        );
    }

    @GetMapping("/access-logs")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get paginated customer tracking link access logs")
    public ResponseEntity<Page<OrderAccessLogResponseDto>> getAccessLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                communicationLogService.getAccessLogs(search, deviceType, startDate, endDate, pageable)
        );
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get paginated staff & system audit logs")
    public ResponseEntity<Page<AuditLogResponseDto>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                communicationLogService.getAuditLogs(search, action, startDate, endDate, pageable)
        );
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get communication & customer engagement statistics")
    public ResponseEntity<CommunicationStatsDto> getStats() {
        return ResponseEntity.ok(communicationLogService.getStats());
    }

    @PostMapping("/whatsapp")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Generate and log a WhatsApp notification message")
    public ResponseEntity<SendWhatsAppResponseDto> sendWhatsAppMessage(
            @Valid @RequestBody SendMessageRequestDto request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "STAFF";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communicationLogService.sendWhatsAppMessage(request, username));
    }
}
