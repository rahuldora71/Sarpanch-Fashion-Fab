package com.sff.authapp.login.dtos.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
public record CompleteOrderMeasurementDto(
        Long measurementId,
        @NotEmpty Map<String, Object> measurementFields,
        Map<String, Object> alteration,
        String note,
        String alterationNote,
        Boolean editable,
        @NotNull Long takenByUserId
) {}