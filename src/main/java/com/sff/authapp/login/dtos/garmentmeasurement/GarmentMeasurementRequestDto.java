package com.sff.authapp.login.dtos.garmentmeasurement;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record GarmentMeasurementRequestDto(

        @NotNull(message = "Garment ID is required")
        Long garmentId,

        @NotEmpty(message = "Measurement fields are required")
        Map<String, Object> measurementFields,

        Map<String, Object> alteration,

        String note,

        String alterationNote,

        Boolean editable
) {
}