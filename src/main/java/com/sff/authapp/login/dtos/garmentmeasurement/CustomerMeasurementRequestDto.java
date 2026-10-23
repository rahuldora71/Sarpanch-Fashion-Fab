package com.sff.authapp.login.dtos.garmentmeasurement;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO for recording a measurement for a customer and tailoring service directly,
 * without requiring an existing order or garment.
 */
public record CustomerMeasurementRequestDto(

        @NotNull(message = "Customer ID is required")
        Long userId,

        @NotNull(message = "Service ID is required")
        Long serviceId,

        @NotEmpty(message = "Measurement fields are required")
        Map<String, Object> measurementFields,

        Map<String, Object> alteration,

        String note,

        String alterationNote,

        Boolean editable,

        Long takenByUserId
) {
}
