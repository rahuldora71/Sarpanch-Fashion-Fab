package com.sff.authapp.login.dtos.measurement;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record MeasurementRequestDto(

        @NotNull(message = "Customer ID is required")
        Long userId,

        @NotNull(message = "Service ID is required")
        Long serviceId,

        @NotEmpty(message = "Measurement fields are required")
        Map<String, Object> measurementFields,

        String note,

        String alterationNote,

        Boolean editable
) {
}