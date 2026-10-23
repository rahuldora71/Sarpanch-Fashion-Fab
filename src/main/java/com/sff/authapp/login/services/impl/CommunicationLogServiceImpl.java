package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.communication.CommunicationDtos.*;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.services.CommunicationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationLogServiceImpl implements CommunicationLogService {

    private final MessageLogRepository messageLogRepository;
    private final OrderAccessLogRepository orderAccessLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<MessageLogResponseDto> getMessageLogs(
            String channel,
            String messageType,
            String status,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanChannel = (channel != null && !channel.equalsIgnoreCase("ALL")) ? channel.toUpperCase() : null;
        String cleanType = (messageType != null && !messageType.equalsIgnoreCase("ALL")) ? messageType.toUpperCase() : null;
        String cleanStatus = (status != null && !status.equalsIgnoreCase("ALL")) ? status.toUpperCase() : null;

        return messageLogRepository.filterMessageLogs(
                cleanChannel,
                cleanType,
                cleanStatus,
                cleanSearch,
                startDt,
                endDt,
                pageable
        ).map(this::mapToMessageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderAccessLogResponseDto> getAccessLogs(
            String search,
            String deviceType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanDevice = (deviceType != null && !deviceType.equalsIgnoreCase("ALL")) ? deviceType.toUpperCase() : null;

        return orderAccessLogRepository.filterAccessLogs(
                cleanSearch,
                cleanDevice,
                startDt,
                endDt,
                pageable
        ).map(this::mapToAccessDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(
            String search,
            String action,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanAction = (action != null && !action.equalsIgnoreCase("ALL")) ? action.toUpperCase() : null;

        return auditLogRepository.filterAuditLogs(
                cleanSearch,
                cleanAction,
                startDt,
                endDt,
                pageable
        ).map(this::mapToAuditDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationStatsDto getStats() {
        long totalMsgs = messageLogRepository.count();
        long whatsappCount = messageLogRepository.countByChannel("WHATSAPP");
        long smsCount = messageLogRepository.countByChannel("SMS");
        long deliveredCount = messageLogRepository.countByStatus("DELIVERED");
        long clickedCount = messageLogRepository.countByStatus("CLICKED");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayMsgs = messageLogRepository.countBySentAtAfter(todayStart);
        long todayAccesses = orderAccessLogRepository.countByAccessedAtAfter(todayStart);
        long totalAccesses = orderAccessLogRepository.count();

        double ctr = (totalMsgs > 0) ? ((double) clickedCount / totalMsgs) * 100.0 : 0.0;

        return CommunicationStatsDto.builder()
                .totalMessages(totalMsgs)
                .whatsappCount(whatsappCount)
                .smsCount(smsCount)
                .deliveredCount(deliveredCount + clickedCount)
                .clickedCount(clickedCount)
                .clickThroughRate(Math.round(ctr * 10.0) / 10.0)
                .todayMessages(todayMsgs)
                .todayAccesses(todayAccesses)
                .totalCustomerAccesses(totalAccesses)
                .build();
    }

    @Override
    @Transactional
    public SendWhatsAppResponseDto sendWhatsAppMessage(SendMessageRequestDto req, String username) {
        Order order = null;
        if (req.getOrderId() != null) {
            order = orderRepository.findById(req.getOrderId()).orElse(null);
        }

        String customerName = req.getCustomerName();
        if ((customerName == null || customerName.isBlank()) && order != null && order.getCustomer() != null) {
            customerName = order.getCustomer().getName();
        }
        if (customerName == null || customerName.isBlank()) {
            customerName = "Valued Customer";
        }

        String phone = req.getPhone();
        if ((phone == null || phone.isBlank()) && order != null && order.getCustomer() != null) {
            phone = order.getCustomer().getPhone();
        }
        if (phone == null || phone.isBlank()) {
            phone = "9876543210";
        }

        // Tracking URL
        String trackingToken = (order != null) ? order.getEffectiveTrackingToken() : "track";
        String trackingUrl = "/track/" + trackingToken;

        // Compose Message Content
        String content = req.getCustomMessage();
        String msgType = req.getMessageType() != null ? req.getMessageType() : "TRACKING_LINK";

        if (content == null || content.isBlank()) {
            content = buildDefaultMessage(msgType, customerName, order, trackingUrl);
        }

        // Clean phone for wa.me (e.g. 919876543210)
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 10) {
            cleanPhone = "91" + cleanPhone;
        }

        String encodedText = URLEncoder.encode(content, StandardCharsets.UTF_8);
        String whatsappUrl = "https://wa.me/" + cleanPhone + "?text=" + encodedText;

        // Save Log
        MessageLog logEntry = MessageLog.builder()
                .orderId(order != null ? order.getId() : req.getOrderId())
                .orderNumber(order != null ? order.getOrderNumber() : null)
                .customerId(req.getCustomerId())
                .customerName(customerName)
                .recipientPhone(phone)
                .channel("WHATSAPP")
                .messageType(msgType)
                .messageContent(content)
                .trackingUrl(trackingUrl)
                .status("SENT")
                .sentBy(username != null ? username : "STAFF")
                .sentAt(LocalDateTime.now())
                .build();

        MessageLog saved = messageLogRepository.save(logEntry);

        return SendWhatsAppResponseDto.builder()
                .logId(saved.getId())
                .whatsappUrl(whatsappUrl)
                .recipientPhone(phone)
                .messageContent(content)
                .trackingUrl(trackingUrl)
                .status("SENT")
                .build();
    }

    @Override
    @Transactional
    public void recordCustomerAccess(String token, String ipAddress, String userAgent) {
        if (token == null || token.isBlank()) return;

        Optional<Order> orderOpt = orderRepository.findByTrackingToken(token);
        if (orderOpt.isEmpty() && token.matches("^\\d+$")) {
            orderOpt = orderRepository.findByOrderNumber(Long.parseLong(token));
        }

        Long orderId = orderOpt.map(Order::getId).orElse(null);
        Long orderNumber = orderOpt.map(Order::getOrderNumber).orElse(null);
        String custName = (orderOpt.isPresent() && orderOpt.get().getCustomer() != null)
                ? orderOpt.get().getCustomer().getName()
                : "Customer (Direct Link)";

        String deviceType = detectDeviceType(userAgent);

        OrderAccessLog accessLog = OrderAccessLog.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .trackingToken(token)
                .customerName(custName)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                .deviceType(deviceType)
                .accessedAt(LocalDateTime.now())
                .build();

        orderAccessLogRepository.save(accessLog);

        // Update corresponding message logs to "CLICKED"
        List<MessageLog> logs = messageLogRepository.findByTrackingUrlContaining(token);
        for (MessageLog m : logs) {
            if ("SENT".equals(m.getStatus()) || "DELIVERED".equals(m.getStatus())) {
                m.setStatus("CLICKED");
                m.setClickedAt(LocalDateTime.now());
                messageLogRepository.save(m);
            }
        }
    }

    private String detectDeviceType(String ua) {
        if (ua == null) return "MOBILE";
        String lower = ua.toLowerCase();
        if (lower.contains("ipad") || lower.contains("tablet") || lower.contains("playbook")) {
            return "TABLET";
        }
        if (lower.contains("mobi") || lower.contains("android") || lower.contains("iphone")) {
            return "MOBILE";
        }
        return "DESKTOP";
    }

    private String buildDefaultMessage(String type, String customerName, Order order, String trackingUrl) {
        String ordNum = (order != null && order.getOrderNumber() != null) ? "#" + order.getOrderNumber() : "";

        return switch (type) {
            case "READY_FOR_TRIAL" ->
                    "Namaste " + customerName + "! ✨ Great news! Your custom tailoring order " + ordNum + " is READY FOR TRIAL / PICKUP at Sarpanch Fashion Fab. 🧵 View details & bill: " + trackingUrl + ". We look forward to seeing you!";
            case "DELIVERED" ->
                    "Namaste " + customerName + "! 🌟 Your order " + ordNum + " has been delivered by Sarpanch Fashion Fab. We hope you love the bespoke fit! Please share your review here: " + trackingUrl + ". Thank you!";
            case "INVOICE_BILL" ->
                    "Namaste " + customerName + "! 🧾 Your tailoring invoice for order " + ordNum + " is available from Sarpanch Fashion Fab. Download PDF & view breakdown: " + trackingUrl + ". Thank you!";
            default ->
                    "Namaste " + customerName + "! 🙏 Your tailoring order " + ordNum + " is confirmed with Sarpanch Fashion Fab. 🧵 Track live tailoring progress, fitting dates & bill anytime here: " + trackingUrl + ". Thank you!";
        };
    }

    private MessageLogResponseDto mapToMessageDto(MessageLog m) {
        return MessageLogResponseDto.builder()
                .id(m.getId())
                .orderId(m.getOrderId())
                .orderNumber(m.getOrderNumber())
                .customerId(m.getCustomerId())
                .customerName(m.getCustomerName())
                .recipientPhone(m.getRecipientPhone())
                .channel(m.getChannel())
                .messageType(m.getMessageType())
                .messageContent(m.getMessageContent())
                .trackingUrl(m.getTrackingUrl())
                .status(m.getStatus())
                .sentBy(m.getSentBy())
                .sentAt(m.getSentAt())
                .clickedAt(m.getClickedAt())
                .build();
    }

    private OrderAccessLogResponseDto mapToAccessDto(OrderAccessLog a) {
        return OrderAccessLogResponseDto.builder()
                .id(a.getId())
                .orderId(a.getOrderId())
                .orderNumber(a.getOrderNumber())
                .trackingToken(a.getTrackingToken())
                .customerName(a.getCustomerName())
                .ipAddress(a.getIpAddress())
                .userAgent(a.getUserAgent())
                .deviceType(a.getDeviceType())
                .accessedAt(a.getAccessedAt())
                .build();
    }

    private AuditLogResponseDto mapToAuditDto(AuditLog l) {
        return AuditLogResponseDto.builder()
                .id(l.getId())
                .userId(l.getUser() != null ? l.getUser().getId() : null)
                .userName(l.getUser() != null ? l.getUser().getName() : "System")
                .action(l.getAction())
                .entityName(l.getEntityName())
                .entityId(l.getEntityId())
                .details(l.getDetails())
                .ipAddress(l.getIpAddress())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
