package com.sff.authapp.login.dtos.production;

import com.sff.authapp.login.entities.BlockedDate;
import com.sff.authapp.login.entities.Holiday;
import com.sff.authapp.login.entities.ProductionCapacity.ProductionStage;
import com.sff.authapp.login.entities.WorkerAttendance.AttendanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ProductionCalendarDtos {

    public record ProductionCalendarResponse(
            LocalDate monthStart,
            LocalDate monthEnd,
            List<DaySummary> days,
            BigDecimal defaultDailyCapacity
    ) {}

    public record DaySummary(
            LocalDate date,
            int dayOfWeek,
            long scheduledGarments,
            long completedGarments,
            long pendingGarments,
            long cuttingPending,
            long stitchingPending,

            BigDecimal maxCapacity,
            BigDecimal effectiveCapacity,
            BigDecimal remainingCapacity,

            // Status indicator: "GREEN", "YELLOW", "RED", "LOCKED", "HOLIDAY"
            String capacityStatus,
            int capacityPercentageUsed,

            boolean isBlocked,
            String blockReason,

            boolean isHoliday,
            String holidayName,
            boolean deliveryAllowed,

            int workersPresent,
            int workersAbsent,
            boolean isPowerCut,
            String capacityNote,

            // Order-level progress counts for this date
            long scheduledOrders,
            long ordersInCutting,
            long ordersInStitching,
            long ordersInIroning,
            long ordersReadyForDelivery,
            long ordersFullyDelivered,
            int averageOrderProgress
    ) {}

    public record DayDetailResponse(
            LocalDate date,
            DaySummary summary,
            List<ScheduledGarmentSummary> garments,
            List<OrderProductionProgressSummary> orders,
            List<WorkerAttendanceSummary> workerAttendance,
            List<CapacityReductionSummary> reductions
    ) {}

    public record OrderProductionProgressSummary(
            Long orderId,
            String customerName,
            String customerPhone,
            String sampleBookPageNumber,
            LocalDate orderDate,
            int totalGarments,
            int cuttingCount,
            int stitchingCount,
            int ironingCount,
            int readyCount,
            int deliveredCount,
            String dominantPhase,
            String dominantPhaseLabel,
            int dominantPhaseGarmentCount,
            int dominantPhaseProgress,
            int overallOrderProgress,
            boolean isFullyDelivered,
            boolean isPartiallyDelivered,
            boolean isAllReadyOrDelivered,
            List<ScheduledGarmentSummary> orderGarments
    ) {}

    public record ScheduledGarmentSummary(
            Long garmentId,
            Long orderId,
            String customerName,
            String customerPhone,
            String serviceName,
            String sampleBookPageNumber,
            String status,
            LocalDate tryDate,
            LocalDate dueDate,
            boolean customerFabric,
            Long madeByWorkerId,
            String madeByWorkerName,
            String cuttingByWorkerName
    ) {}

    public record WorkerAttendanceSummary(
            Long workerId,
            String workerName,
            String role,
            AttendanceStatus status,
            int capacityPercentage,
            String note
    ) {}

    public record CapacityReductionSummary(
            Long capacityId,
            ProductionStage stage,
            BigDecimal maxCapacity,
            BigDecimal reducedCapacity,
            String reason
    ) {}

    public record BlockDateRequest(
            LocalDate date,
            String reason,
            boolean deliveryAllowed,
            boolean workersWorking,
            int capacityPercentage
    ) {}

    public record HolidayRequest(
            LocalDate date,
            String name,
            String description,
            boolean deliveryAllowed,
            boolean workersWorking,
            int capacityPercentage,
            boolean recurring
    ) {}

    public record WorkerAttendanceRequest(
            Long workerId,
            LocalDate date,
            AttendanceStatus status,
            int capacityPercentage,
            String note
    ) {}

    public record CapacityConfigRequest(
            LocalDate date,
            ProductionStage stage,
            Long workerId,
            BigDecimal maxCapacity,
            boolean capacityReduced,
            String reductionReason
    ) {}
}
