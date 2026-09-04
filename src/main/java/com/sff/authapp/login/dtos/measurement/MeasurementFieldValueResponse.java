package com.sff.authapp.login.dtos.measurement;

public record MeasurementFieldValueResponse(

        Long fieldId,

        String fieldName,

        Object value
) {
}