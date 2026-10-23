package com.sff.authapp.login.dtos.order;

import com.sff.authapp.login.entities.GarmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OrderDetailsResponse(
        Long orderId,
        LocalDate orderDate,
        String sampleBookPageNumber,

        CustomerDetails customer,

        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal additionalCharges,
        BigDecimal grandTotal,
        BigDecimal totalPaid,
        BigDecimal balanceDue,

        List<GarmentDetails> garments,
        List<ChargeDetails> charges,
        List<PaymentDetails> payments,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record CustomerDetails(
            Long id,
            String customerCode,
            String name,
            String phone,
            String email,
            String image
    ) {}

    public record GarmentDetails(
            Long garmentId,
            Long serviceId,
            String serviceName,
            BigDecimal price,
            String sampleNumber,
            GarmentStatus status,
            boolean customerFabric,
            LocalDate tryDate,
            LocalDate dueDate,
            LocalDate alterationDeliveryDate,
            String alterationNote,
            String note,

            Long cuttingByWorkerId,
            String cuttingByWorkerName,
            Long madeByWorkerId,
            String madeByWorkerName,
            String madeByWorkerPhone,
            String madeByWorkerImage,

            MeasurementDetails measurement,

            List<AddOnDetails> addOns
    ) {}

    public record MeasurementDetails(
            Long measurementId,
            Map<String, Object> measurementFields,
            Map<String, Object> alteration,
            Long takenByUserId,
            boolean editable,
            String note,
            String alterationNote,
            LocalDateTime measuredAt
    ) {}

    public record AddOnDetails(
            Long garmentAddOnId,
            Long addOnId,
            String name,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String note
    ) {}

    public record ChargeDetails(
            Long chargeId,
            String description,
            BigDecimal amount
    ) {}

    public record PaymentDetails(
            Long paymentId,
            BigDecimal amount,
            String paymentMethod,
            String paymentType,
            String note,
            Long receivedById,
            LocalDateTime createdAt
    ) {}
}