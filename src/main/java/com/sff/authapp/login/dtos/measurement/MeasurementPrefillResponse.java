package com.sff.authapp.login.dtos.measurement;

import java.time.LocalDateTime;
import java.util.List;

public record MeasurementPrefillResponse(

        boolean hasLatestMeasurement,

        Long sourceMeasurementId,

        Long sourceServiceId,

        String sourceServiceName,

        LocalDateTime measuredAt,

        List<MeasurementFieldValueResponse> fields
) {
}