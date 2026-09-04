package com.sff.authapp.login.dtos.garment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GarmentMeasurementResponse(

        Long id,

        Long garmentId,

        Long orderId,

        String orderNumber,

        Long customerId,

        String customerName,

        // Upper body

        BigDecimal chest,

        BigDecimal waist,

        BigDecimal hip,

        BigDecimal shoulder,

        BigDecimal neck,

        BigDecimal armLength,

        BigDecimal sleeveLength,

        BigDecimal bicep,

        BigDecimal wrist,

        BigDecimal armhole,

        BigDecimal shirtLength,

        BigDecimal frontLength,

        BigDecimal backLength,

        // Lower body

        BigDecimal pantWaist,

        BigDecimal pantLength,

        BigDecimal inseam,

        BigDecimal outseam,

        BigDecimal thigh,

        BigDecimal knee,

        BigDecimal bottomOpening,

        BigDecimal rise,

        // Additional

        String notes,

        // Taken by

        Long takenById,

        String takenByName,

        // Audit

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}