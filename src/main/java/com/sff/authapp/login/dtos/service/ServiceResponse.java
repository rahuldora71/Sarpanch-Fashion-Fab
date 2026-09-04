package com.sff.authapp.login.dtos.service;

import com.sff.authapp.login.entities.MeasurementField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ServiceResponse(

        Long id,

        String name,

        String category,

        BigDecimal price,

        boolean active,

        BigDecimal tailorCommission,

        List<MeasurementField> measurementFields,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}