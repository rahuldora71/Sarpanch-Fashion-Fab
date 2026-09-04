package com.sff.authapp.login.dtos.garment;

import com.sff.authapp.login.entities.GarmentStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GarmentUpdateRequestDto(

        Long serviceId,

        @Size(
                max = 50,
                message = "Sample number cannot exceed 50 characters"
        )
        String sampleNumber,

        Boolean customerFabric,

        LocalDate tryDate,

        LocalDate dueDate,

        GarmentStatus status,

        @Size(
                max = 5000,
                message = "Note cannot exceed 5000 characters"
        )
        String note,

        @Size(
                max = 5000,
                message = "Alteration note cannot exceed 5000 characters"
        )
        String alterationNote,

        LocalDate alterationDeliveryDate
) {
}