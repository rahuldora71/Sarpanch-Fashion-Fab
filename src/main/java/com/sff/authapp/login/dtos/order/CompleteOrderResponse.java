package com.sff.authapp.login.dtos.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompleteOrderResponse(

        Long orderId,

        Long customerId,

        String customerName,

        String customerCode,

        LocalDate orderDate,

        String sampleBookPageNumber,

        BigDecimal discount,

        BigDecimal subtotal,

        BigDecimal additionalCharges,

        BigDecimal grandTotal,

        BigDecimal paymentAmount,

        BigDecimal balanceDue,

        String paymentMethod,

        String paymentType,

        List<GarmentCreatedResponse> garments,

        LocalDateTime createdAt

) {

    public record GarmentCreatedResponse(

            Long garmentId,

            Long serviceId,

            String serviceName,

            BigDecimal price,

            String sampleNumber,

            Long measurementId,

            List<Long> garmentAddOnIds

    ) {}
}