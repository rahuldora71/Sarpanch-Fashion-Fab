package com.sff.authapp.login.dtos.garmentaddon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GarmentAddOnResponse(

        Long id,

        Long garmentId,

        Long addOnId,

        String addOnName,

        BigDecimal unitPrice,

        Integer quantity,

        BigDecimal totalPrice,

        BigDecimal tailorCommission,

        String note,

        LocalDateTime createdAt
) {
}