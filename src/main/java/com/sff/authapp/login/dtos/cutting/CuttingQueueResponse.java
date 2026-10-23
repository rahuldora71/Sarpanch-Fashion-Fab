package com.sff.authapp.login.dtos.cutting;

import com.sff.authapp.login.entities.GarmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for the Cutting Queue API.
 * Groups garments that need cutting by delivery date,
 * then by order/customer for the production UI.
 */
public record CuttingQueueResponse(
        List<CuttingDateGroup> dateGroups,
        int totalGarmentsPending,
        int totalOrdersPending,
        LocalDate fetchedAt
) {

    /**
     * All pending-cutting garments grouped under one delivery date.
     */
    public record CuttingDateGroup(
            LocalDate dueDate,
            String dueDateLabel,  // e.g. "Today", "Tomorrow", "26 Sep"
            boolean isOverdue,
            boolean isToday,
            boolean isTomorrow,
            int garmentCount,
            List<CuttingOrderGroup> orders
    ) {}

    /**
     * All pending-cutting garments for a single order,
     * shown under their due-date group.
     */
    public record CuttingOrderGroup(
            Long orderId,
            String sampleBookPageNumber,
            LocalDate orderDate,
            CustomerInfo customer,
            int garmentCount,
            List<CuttingGarmentItem> garments
    ) {}

    /**
     * Customer information for the cutting workspace header.
     */
    public record CustomerInfo(
            Long customerId,
            String customerCode,
            String name,
            String phone,
            String image
    ) {}

    /**
     * One garment item in the cutting queue.
     */
    public record CuttingGarmentItem(
            Long garmentId,
            Long serviceId,
            String serviceName,
            BigDecimal price,
            String sampleNumber,
            GarmentStatus status,
            boolean customerFabric,
            LocalDate dueDate,
            LocalDate tryDate,
            String note,

            // Measurements ready to display
            MeasurementInfo measurement,

            // Add-ons, separated by production stage
            List<AddOnInfo> cuttingAddOns,   // beforeCutting = true
            List<AddOnInfo> stitchingAddOns, // beforeCutting = false

            // Worker currently assigned for cutting
            Long cuttingByUserId,
            String cuttingByUserName,

            LocalDateTime createdAt
    ) {}

    /**
     * Measurement details for cutting display.
     */
    public record MeasurementInfo(
            Long measurementId,
            Map<String, Object> fields,
            String note,
            Long takenByUserId,
            LocalDateTime measuredAt
    ) {}

    /**
     * Add-on information enriched with production-stage context.
     */
    public record AddOnInfo(
            Long garmentAddOnId,
            Long addOnId,
            String name,
            String imageUrl,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            boolean beforeCutting,
            String cuttingInstruction,
            String fabricMarkingNote,
            String note
    ) {}
}
