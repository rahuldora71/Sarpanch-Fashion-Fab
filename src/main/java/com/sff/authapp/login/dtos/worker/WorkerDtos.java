package com.sff.authapp.login.dtos.worker;

import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.entities.WorkerAttendance.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class WorkerDtos {

    public record WorkerRequestDto(
            @NotBlank(message = "Worker name is required")
            String name,

            @NotBlank(message = "Phone number is required")
            String phone,

            String alternatePhone,

            @PositiveOrZero(message = "Daily capacity must be 0 or greater")
            BigDecimal dailyCapacity,

            String workerSpecialty,

            String imageUrl,

            String imagePublicId,

            String address,

            String city,

            String notes,

            Boolean active
    ) {}

    public record WorkerResponseDto(
            Long id,
            String name,
            String phone,
            String alternatePhone,
            BigDecimal dailyCapacity,
            String workerSpecialty,
            String imageUrl,
            String imagePublicId,
            String address,
            String city,
            String notes,
            boolean active,
            AttendanceStatus todayAttendanceStatus,
            int todayCapacityPercentage,
            Instant createdAt
    ) {}

    public record AllocationAddOnDto(
            Long id,
            String name,
            Integer quantity,
            String imageUrl,
            String note,
            BigDecimal tailorCommission
    ) {}

    public record WorkAllocationItemDto(
            Long garmentId,
            Long orderId,
            LocalDate orderDate,
            String sampleBookPageNumber,
            int garmentIndexInOrder,
            int totalGarmentsInOrder,
            String customerName,
            String customerPhone,
            Long serviceId,
            String serviceName,
            String sampleNumber,
            GarmentStatus status,
            boolean customerFabric,
            LocalDate tryDate,
            LocalDate dueDate,
            String note,
            Long cuttingByWorkerId,
            String cuttingByWorkerName,
            Long madeByWorkerId,
            String madeByWorkerName,
            String madeByWorkerPhone,
            String madeByWorkerImage,
            String madeByWorkerSpecialty,
            List<AllocationAddOnDto> addOns,
            BigDecimal garmentPrice,
            BigDecimal tailorCommission,
            java.time.LocalDateTime updatedAt,
            java.time.LocalDateTime createdAt
    ) {}

    public record AllocateWorkRequestDto(
            @NotNull(message = "Worker ID is required")
            Long workerId,

            @NotEmpty(message = "At least one garment must be selected")
            List<Long> garmentIds,

            String note
    ) {}

    public record WorkerAdvanceDto(
            Long id,
            Long workerId,
            String workerName,
            BigDecimal amount,
            LocalDate advanceDate,
            String type,
            String note,
            String paymentMode,
            Long settlementId,
            java.time.LocalDateTime createdAt
    ) {}

    public record CreateWorkerAdvanceRequestDto(
            @NotNull(message = "Worker ID is required")
            Long workerId,

            @NotNull(message = "Amount is required")
            BigDecimal amount,

            @NotNull(message = "Advance date is required")
            LocalDate advanceDate,

            String type,
            String note,
            String paymentMode
    ) {}

    public record WorkerSettlementDto(
            Long id,
            String settlementNumber,
            Long workerId,
            String workerName,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate settlementDate,
            String cycleLabel,
            Integer totalGarmentsCount,
            BigDecimal grossWorkAmount,
            BigDecimal previousBalance,
            BigDecimal totalAdvancesDeducted,
            BigDecimal netPayable,
            BigDecimal amountPaid,
            BigDecimal closingBalance,
            String paymentMode,
            String note,
            String garmentBreakdownJson,
            java.time.LocalDateTime createdAt
    ) {}

    public record CreateWorkerSettlementRequestDto(
            @NotNull(message = "Worker ID is required")
            Long workerId,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate,

            @NotNull(message = "Settlement date is required")
            LocalDate settlementDate,

            String cycleLabel,
            Integer totalGarmentsCount,
            BigDecimal grossWorkAmount,
            BigDecimal previousBalance,
            BigDecimal totalAdvancesDeducted,
            BigDecimal netPayable,
            BigDecimal amountPaid,
            BigDecimal closingBalance,
            String paymentMode,
            String note,
            String garmentBreakdownJson
    ) {}
}
