package com.sff.authapp.login.dtos.garmentmeasurement;

import java.time.LocalDateTime;
import java.util.Map;

public record GarmentMeasurementResponse(

        Long id,

        Long garmentId,

        Long orderId,

        Long customerId,

        Long serviceId,

        String serviceName,

        Map<String, Object> measurementFields,

        Map<String, Object> alteration,

        Long takenByUserId,

        boolean editable,

        String note,

        String alterationNote,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}