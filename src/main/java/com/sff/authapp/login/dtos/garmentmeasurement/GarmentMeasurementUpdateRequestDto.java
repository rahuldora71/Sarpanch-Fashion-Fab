package com.sff.authapp.login.dtos.garmentmeasurement;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record GarmentMeasurementUpdateRequestDto(

        @NotEmpty(message = "Measurement fields are required")
        Map<String, Object> measurementFields,

        Map<String, Object> alteration,

        String note,

        String alterationNote,

        Boolean editable
) {
}