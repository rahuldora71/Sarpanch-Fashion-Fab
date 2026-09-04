package com.sff.authapp.login.dtos.measurement;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record MeasurementUpdateRequestDto(

        @NotEmpty(message = "Measurement fields are required")
        Map<String, Object> measurementFields,

        String note,

        String alterationNote,

        Boolean editable
) {
}