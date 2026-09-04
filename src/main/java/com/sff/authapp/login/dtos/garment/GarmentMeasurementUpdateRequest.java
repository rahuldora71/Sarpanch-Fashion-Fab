package com.sff.authapp.login.dtos.garment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GarmentMeasurementUpdateRequest(

        @DecimalMin(
                value = "0.00",
                message = "Chest cannot be negative"
        )
        BigDecimal chest,

        @DecimalMin(
                value = "0.00",
                message = "Waist cannot be negative"
        )
        BigDecimal waist,

        @DecimalMin(
                value = "0.00",
                message = "Hip cannot be negative"
        )
        BigDecimal hip,

        @DecimalMin(
                value = "0.00",
                message = "Shoulder cannot be negative"
        )
        BigDecimal shoulder,

        @DecimalMin(
                value = "0.00",
                message = "Neck cannot be negative"
        )
        BigDecimal neck,

        @DecimalMin(
                value = "0.00",
                message = "Arm length cannot be negative"
        )
        BigDecimal armLength,

        @DecimalMin(
                value = "0.00",
                message = "Sleeve length cannot be negative"
        )
        BigDecimal sleeveLength,

        @DecimalMin(
                value = "0.00",
                message = "Bicep cannot be negative"
        )
        BigDecimal bicep,

        @DecimalMin(
                value = "0.00",
                message = "Wrist cannot be negative"
        )
        BigDecimal wrist,

        @DecimalMin(
                value = "0.00",
                message = "Armhole cannot be negative"
        )
        BigDecimal armhole,

        @DecimalMin(
                value = "0.00",
                message = "Shirt length cannot be negative"
        )
        BigDecimal shirtLength,

        @DecimalMin(
                value = "0.00",
                message = "Front length cannot be negative"
        )
        BigDecimal frontLength,

        @DecimalMin(
                value = "0.00",
                message = "Back length cannot be negative"
        )
        BigDecimal backLength,

        @DecimalMin(
                value = "0.00",
                message = "Pant waist cannot be negative"
        )
        BigDecimal pantWaist,

        @DecimalMin(
                value = "0.00",
                message = "Pant length cannot be negative"
        )
        BigDecimal pantLength,

        @DecimalMin(
                value = "0.00",
                message = "Inseam cannot be negative"
        )
        BigDecimal inseam,

        @DecimalMin(
                value = "0.00",
                message = "Outseam cannot be negative"
        )
        BigDecimal outseam,

        @DecimalMin(
                value = "0.00",
                message = "Thigh cannot be negative"
        )
        BigDecimal thigh,

        @DecimalMin(
                value = "0.00",
                message = "Knee cannot be negative"
        )
        BigDecimal knee,

        @DecimalMin(
                value = "0.00",
                message = "Bottom opening cannot be negative"
        )
        BigDecimal bottomOpening,

        @DecimalMin(
                value = "0.00",
                message = "Rise cannot be negative"
        )
        BigDecimal rise,

        @Size(
                max = 2000,
                message = "Notes cannot exceed 2000 characters"
        )
        String notes,

        Long takenById
) {
}