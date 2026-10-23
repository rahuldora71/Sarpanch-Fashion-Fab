package com.sff.authapp.login.dtos.garmentmeasurement;

import java.time.LocalDateTime;
import java.util.Map;

public record GarmentMeasurementSummaryResponse(

        Long measurementId,

        Long garmentId,

        Long orderId,

        Long serviceId,

        String serviceName,

        Map<String, Object> measurementFields,

        LocalDateTime measuredAt
) {
}