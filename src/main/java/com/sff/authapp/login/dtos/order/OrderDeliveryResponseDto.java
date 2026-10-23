package com.sff.authapp.login.dtos.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderDeliveryResponseDto(
        Long orderId,
        int totalGarments,
        int deliveredGarmentsCount,
        boolean fullyDelivered,
        List<Long> deliveredGarmentIds,
        BigDecimal grandTotal,
        BigDecimal totalPaid,
        BigDecimal balanceDue,
        boolean paymentRecorded,
        String message
) {
}
