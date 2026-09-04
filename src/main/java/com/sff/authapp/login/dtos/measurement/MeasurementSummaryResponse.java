package com.sff.authapp.login.dtos.measurement;

import java.time.LocalDateTime;
import java.util.Map;

public record MeasurementSummaryResponse(

        Long measurementId,

        Long serviceId,

        String serviceName,

        Map<String, Object> measurementFields,

        LocalDateTime measuredAt
) {
}