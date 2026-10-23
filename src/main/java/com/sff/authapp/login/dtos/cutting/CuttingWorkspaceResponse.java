package com.sff.authapp.login.dtos.cutting;

import com.sff.authapp.login.entities.GarmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for the Cutting Workspace -
 * the detailed view opened when a cutter selects an order from the queue.
 */
public record CuttingWorkspaceResponse(
        Long orderId,
        String sampleBookPageNumber,
        LocalDate orderDate,

        CuttingQueueResponse.CustomerInfo customer,

        List<CuttingWorkspaceGarment> garments,

        LocalDateTime fetchedAt
) {

    /**
     * Full garment details for the cutting workspace, including
     * measurements, add-ons, and worker information.
     */
    public record CuttingWorkspaceGarment(
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

            // Already-cut or still pending
            boolean pendingCutting,

            // Measurement fields
            CuttingQueueResponse.MeasurementInfo measurement,

            // Add-ons that require action during cutting
            List<CuttingQueueResponse.AddOnInfo> cuttingAddOns,

            // Other add-ons (stitching, etc.) shown for reference
            List<CuttingQueueResponse.AddOnInfo> otherAddOns,

            // Assigned cutting worker
            Long cuttingByUserId,
            String cuttingByUserName,

            // Cut completed timestamp (null if not yet cut)
            LocalDateTime cutAt
    ) {}
}
