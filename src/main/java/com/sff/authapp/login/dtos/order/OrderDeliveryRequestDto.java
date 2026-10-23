package com.sff.authapp.login.dtos.order;

import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record OrderDeliveryRequestDto(
        @NotEmpty(message = "Please select at least one garment to deliver")
        List<Long> garmentIds,

        Long deliveredById,

        String deliveryNote,

        BigDecimal paymentAmount,

        PaymentMethod paymentMethod,

        PaymentType paymentType,

        String paymentNote
) {
}
