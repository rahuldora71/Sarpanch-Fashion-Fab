package com.sff.authapp.login.dtos.garment;

import com.sff.authapp.login.entities.GarmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GarmentResponse(

        Long id,

        Long orderId,

        Long serviceId,

        String serviceName,

        BigDecimal price,

        String sampleNumber,

        Long garmentMeasurementId,

        GarmentStatus status,

        boolean customerFabric,

        LocalDate tryDate,

        LocalDate dueDate,

        Long cuttingByUserId,

        Long madeByUserId,

        Long deliveredByUserId,

        LocalDate alterationDeliveryDate,

        String alterationNote,

        String note,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}