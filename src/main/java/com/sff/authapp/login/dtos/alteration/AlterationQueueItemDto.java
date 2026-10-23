package com.sff.authapp.login.dtos.alteration;

import com.sff.authapp.login.entities.GarmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record AlterationQueueItemDto(
        Long garmentId,
        Long orderId,
        String orderNumber,
        LocalDate orderDate,
        String sampleBookPageNumber,

        Long customerId,
        String customerName,
        String customerPhone,
        String customerCode,

        Long serviceId,
        String serviceName,
        String sampleNumber,
        GarmentStatus status,

        LocalDate alterationDeliveryDate,
        String alterationNote,
        Map<String, Object> alterationDeltas,
        Map<String, Object> measurementFields,

        BigDecimal price,
        Long garmentMeasurementId,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
