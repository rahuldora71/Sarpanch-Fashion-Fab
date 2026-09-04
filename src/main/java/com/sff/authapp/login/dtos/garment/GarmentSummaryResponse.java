package com.sff.authapp.login.dtos.garment;

import com.sff.authapp.login.entities.GarmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GarmentSummaryResponse(

        Long id,

        Long serviceId,

        String serviceName,

        BigDecimal price,

        String sampleNumber,

        GarmentStatus status,

        boolean customerFabric,

        LocalDate dueDate
) {
}