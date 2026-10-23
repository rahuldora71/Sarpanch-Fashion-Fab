package com.sff.authapp.login.dtos.alteration;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

public record GarmentAlterationItemDto(
        @NotNull(message = "Garment ID is required")
        Long garmentId,

        @NotNull(message = "Service ID is required")
        Long serviceId,

        String alterationNote,

        LocalDate alterationDeliveryDate,

        Map<String, Object> updatedMeasurementFields,

        Map<String, Object> alterationDeltas
) {
}
