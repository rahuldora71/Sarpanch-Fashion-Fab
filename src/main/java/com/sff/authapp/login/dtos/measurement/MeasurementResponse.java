package com.sff.authapp.login.dtos.measurement;

import java.time.LocalDateTime;
import java.util.Map;

public record MeasurementResponse(

        Long id,

        Long userId,

        Long serviceId,

        String serviceName,

        Map<String, Object> measurementFields,

        Long takenByUserId,

        boolean editable,

        String note,

        String alterationNote,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}