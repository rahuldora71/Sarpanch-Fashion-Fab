package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.production.ProductionCalendarDtos.*;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.entities.ProductionCapacity.ProductionStage;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.services.ProductionCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductionCalendarServiceImpl implements ProductionCalendarService {

    private final ProductionCapacityRepository capacityRepository;
    private final BlockedDateRepository blockedDateRepository;
    private final HolidayRepository holidayRepository;
    private final WorkerAttendanceRepository attendanceRepository;
    private final GarmentRepository garmentRepository;
    private final UserRepository userRepository;

    // Default daily shop capacity when no override is set
    private static final BigDecimal DEFAULT_DAILY_CAPACITY = new BigDecimal("15.00");

    // ============================================================
    // MONTHLY CALENDAR
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ProductionCalendarResponse getMonthlyCalendar(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // Fetch all blocked dates, holidays, capacities, and attendance in this month
        List<BlockedDate> blockedDates = blockedDateRepository.findByBlockedDateBetweenOrderByBlockedDateAsc(start, end);
        Map<LocalDate, BlockedDate> blockedMap = blockedDates.stream()
                .collect(Collectors.toMap(BlockedDate::getBlockedDate, b -> b, (b1, b2) -> b1));

        List<Holiday> holidays = holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end);
        Map<LocalDate, Holiday> holidayMap = holidays.stream()
                .collect(Collectors.toMap(Holiday::getHolidayDate, h -> h, (h1, h2) -> h1));

        List<ProductionCapacity> capacities = capacityRepository.findByCapacityDateBetweenAndActiveTrue(start, end);
        Map<LocalDate, List<ProductionCapacity>> capacityMap = capacities.stream()
                .collect(Collectors.groupingBy(ProductionCapacity::getCapacityDate));

        List<DaySummary> days = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            days.add(buildDaySummary(date, blockedMap.get(date), holidayMap.get(date), capacityMap.get(date)));
        }

        return new ProductionCalendarResponse(
                start,
                end,
                days,
                DEFAULT_DAILY_CAPACITY
        );
    }

    // ============================================================
    // DAY DETAILS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public DayDetailResponse getDayDetails(LocalDate date) {
        BlockedDate blocked = blockedDateRepository.findByBlockedDate(date).orElse(null);
        Holiday holiday = holidayRepository.findByHolidayDate(date).orElse(null);
        List<ProductionCapacity> caps = capacityRepository.findByCapacityDateAndActiveTrue(date);

        DaySummary summary = buildDaySummary(date, blocked, holiday, caps);

        // Fetch garments due on this date
        List<Garment> garments = garmentRepository.findAll().stream()
                .filter(g -> date.equals(g.getDueDate()))
                .toList();

        List<ScheduledGarmentSummary> garmentSummaries = garments.stream()
                .map(this::toScheduledGarmentSummary)
                .toList();

        // Group distinct Orders that have garments due on this date
        Map<Long, Order> distinctOrders = new LinkedHashMap<>();
        for (Garment g : garments) {
            if (g.getOrder() != null) {
                distinctOrders.putIfAbsent(g.getOrder().getId(), g.getOrder());
            }
        }

        List<OrderProductionProgressSummary> orderProgressSummaries = distinctOrders.values().stream()
                .map(this::buildOrderProgress)
                .toList();

        // Worker attendance
        List<WorkerAttendance> attendanceList = attendanceRepository.findByAttendanceDateOrderByWorkerIdAsc(date);
        List<WorkerAttendanceSummary> attendanceSummaries = attendanceList.stream()
                .map(wa -> {
                    String roleName = wa.getWorker().getRoles() != null && !wa.getWorker().getRoles().isEmpty()
                            ? wa.getWorker().getRoles().iterator().next().getName()
                            : "WORKER";
                    return new WorkerAttendanceSummary(
                            wa.getWorker().getId(),
                            wa.getWorker().getName(),
                            roleName,
                            wa.getStatus(),
                            wa.getCapacityPercentage(),
                            wa.getNote()
                    );
                })
                .toList();

        // Reductions
        List<CapacityReductionSummary> reductions = caps.stream()
                .filter(ProductionCapacity::isCapacityReduced)
                .map(pc -> new CapacityReductionSummary(
                        pc.getId(),
                        pc.getProductionStage(),
                        pc.getMaxCapacity(),
                        pc.getAvailableCapacity(),
                        pc.getReductionReason()
                ))
                .toList();

        return new DayDetailResponse(
                date,
                summary,
                garmentSummaries,
                orderProgressSummaries,
                attendanceSummaries,
                reductions
        );
    }

    // ============================================================
    // BLOCKED DATES
    // ============================================================

    @Override
    public BlockedDate blockDate(BlockDateRequest request, Long userId) {
        Optional<BlockedDate> existing = blockedDateRepository.findByBlockedDate(request.date());
        if (existing.isPresent()) {
            BlockedDate b = existing.get();
            b.setReason(request.reason());
            b.setDeliveryAllowed(request.deliveryAllowed());
            b.setWorkersWorking(request.workersWorking());
            b.setCapacityPercentage(request.capacityPercentage());
            return blockedDateRepository.save(b);
        }

        BlockedDate b = BlockedDate.builder()
                .blockedDate(request.date())
                .reason(request.reason())
                .deliveryAllowed(request.deliveryAllowed())
                .workersWorking(request.workersWorking())
                .capacityPercentage(request.capacityPercentage())
                .createdBy(userId != null ? userId : 1L)
                .build();

        return blockedDateRepository.save(b);
    }

    @Override
    public void unblockDate(Long id) {
        BlockedDate b = blockedDateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked date not found with id: " + id));
        blockedDateRepository.delete(b);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedDate> getAllBlockedDates() {
        return blockedDateRepository.findByBlockedDateGreaterThanEqualOrderByBlockedDateAsc(
                LocalDate.now().minusMonths(1)
        );
    }

    // ============================================================
    // HOLIDAYS
    // ============================================================

    @Override
    public Holiday createHoliday(HolidayRequest request) {
        Holiday h = Holiday.builder()
                .holidayDate(request.date())
                .name(request.name())
                .description(request.description())
                .deliveryAllowed(request.deliveryAllowed())
                .workersWorking(request.workersWorking())
                .capacityPercentage(request.capacityPercentage())
                .recurring(request.recurring())
                .build();
        return holidayRepository.save(h);
    }

    @Override
    public Holiday updateHoliday(Long id, HolidayRequest request) {
        Holiday h = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        h.setHolidayDate(request.date());
        h.setName(request.name());
        h.setDescription(request.description());
        h.setDeliveryAllowed(request.deliveryAllowed());
        h.setWorkersWorking(request.workersWorking());
        h.setCapacityPercentage(request.capacityPercentage());
        h.setRecurring(request.recurring());
        return holidayRepository.save(h);
    }

    @Override
    public void deleteHoliday(Long id) {
        Holiday h = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        holidayRepository.delete(h);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(
                LocalDate.now().minusMonths(1)
        );
    }

    // ============================================================
    // WORKER ATTENDANCE & CAPACITY
    // ============================================================

    @Override
    public WorkerAttendance recordWorkerAttendance(WorkerAttendanceRequest request) {
        User worker = userRepository.findById(request.workerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + request.workerId()));

        Optional<WorkerAttendance> existing = attendanceRepository.findByWorkerIdAndAttendanceDate(
                request.workerId(), request.date()
        );

        WorkerAttendance wa;
        if (existing.isPresent()) {
            wa = existing.get();
            wa.setStatus(request.status());
            wa.setCapacityPercentage(request.capacityPercentage());
            wa.setNote(request.note());
        } else {
            wa = WorkerAttendance.builder()
                    .worker(worker)
                    .attendanceDate(request.date())
                    .status(request.status())
                    .capacityPercentage(request.capacityPercentage())
                    .note(request.note())
                    .build();
        }

        return attendanceRepository.save(wa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerAttendance> getWorkerAttendanceForDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDateOrderByWorkerIdAsc(date);
    }

    @Override
    public ProductionCapacity setCapacityConfig(CapacityConfigRequest request) {
        User worker = null;
        if (request.workerId() != null) {
            worker = userRepository.findById(request.workerId()).orElse(null);
        }

        Optional<ProductionCapacity> existing;
        if (worker != null) {
            existing = capacityRepository.findByDateStageAndWorker(request.date(), request.stage(), worker.getId());
        } else {
            existing = capacityRepository.findByCapacityDateAndProductionStageAndWorkerIsNull(request.date(), request.stage());
        }

        ProductionCapacity pc;
        if (existing.isPresent()) {
            pc = existing.get();
            pc.setMaxCapacity(request.maxCapacity());
            pc.setCapacityReduced(request.capacityReduced());
            pc.setReductionReason(request.reductionReason());
        } else {
            pc = ProductionCapacity.builder()
                    .capacityDate(request.date())
                    .productionStage(request.stage())
                    .worker(worker)
                    .maxCapacity(request.maxCapacity())
                    .availableCapacity(request.maxCapacity())
                    .capacityReduced(request.capacityReduced())
                    .reductionReason(request.reductionReason())
                    .active(true)
                    .build();
        }

        return capacityRepository.save(pc);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeliveryAllowed(LocalDate date) {
        Optional<BlockedDate> blocked = blockedDateRepository.findByBlockedDate(date);
        if (blocked.isPresent() && !blocked.get().isDeliveryAllowed()) {
            return false;
        }

        Optional<Holiday> holiday = holidayRepository.findByHolidayDate(date);
        if (holiday.isPresent() && !holiday.get().isDeliveryAllowed()) {
            return false;
        }

        return true;
    }

    // ============================================================
    // BUILD DAY SUMMARY
    // ============================================================

    private DaySummary buildDaySummary(
            LocalDate date,
            BlockedDate blocked,
            Holiday holiday,
            List<ProductionCapacity> capacities
    ) {
        boolean isBlocked = (blocked != null && !blocked.isDeliveryAllowed());
        String blockReason = blocked != null ? blocked.getReason() : null;

        boolean isHoliday = (holiday != null);
        String holidayName = holiday != null ? holiday.getName() : null;
        boolean deliveryAllowed = !isBlocked && (holiday == null || holiday.isDeliveryAllowed());

        // Base max capacity: sum of active workers' dailyCapacity if workers exist, else DEFAULT_DAILY_CAPACITY
        List<User> activeWorkers = userRepository.findAllWorkersList().stream()
                .filter(User::isEnable)
                .toList();

        BigDecimal workerTotalCap = activeWorkers.stream()
                .map(w -> w.getDailyCapacity() != null ? w.getDailyCapacity() : new BigDecimal("5.00"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxCap = workerTotalCap.compareTo(BigDecimal.ZERO) > 0
                ? workerTotalCap
                : DEFAULT_DAILY_CAPACITY;

        if (capacities != null && !capacities.isEmpty()) {
            Optional<ProductionCapacity> generalStitching = capacities.stream()
                    .filter(c -> c.getProductionStage() == ProductionStage.STITCHING && c.getWorker() == null)
                    .findFirst();
            if (generalStitching.isPresent()) {
                maxCap = generalStitching.get().getMaxCapacity();
            }
        }

        // Apply reductions if power cut or worker absences
        BigDecimal effectiveCap = maxCap;
        boolean isPowerCut = false;
        String reductionNote = null;

        // Subtract capacity for workers marked ABSENT / LEAVE / HALF_DAY on this date
        if (!activeWorkers.isEmpty()) {
            List<WorkerAttendance> dayAtt = attendanceRepository.findByAttendanceDateOrderByWorkerIdAsc(date);
            BigDecimal lostWorkerCapacity = BigDecimal.ZERO;
            for (WorkerAttendance wa : dayAtt) {
                if (wa.getWorker() != null && wa.getWorker().isEnable()) {
                    BigDecimal wCap = wa.getWorker().getDailyCapacity() != null
                            ? wa.getWorker().getDailyCapacity()
                            : new BigDecimal("5.00");
                    int pct = Math.max(0, Math.min(100, wa.getCapacityPercentage()));
                    if (pct < 100) {
                        BigDecimal lost = wCap.multiply(BigDecimal.valueOf(100 - pct))
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        lostWorkerCapacity = lostWorkerCapacity.add(lost);
                    }
                }
            }
            if (lostWorkerCapacity.compareTo(BigDecimal.ZERO) > 0) {
                effectiveCap = effectiveCap.subtract(lostWorkerCapacity);
                if (effectiveCap.compareTo(BigDecimal.ZERO) < 0) {
                    effectiveCap = BigDecimal.ZERO;
                }
            }
        }

        if (capacities != null) {
            for (ProductionCapacity pc : capacities) {
                if (pc.isCapacityReduced()) {
                    isPowerCut = true;
                    reductionNote = pc.getReductionReason();
                    if (pc.getAvailableCapacity() != null) {
                        effectiveCap = pc.getAvailableCapacity();
                    }
                }
            }
        }

        if (holiday != null && holiday.getCapacityPercentage() < 100) {
            effectiveCap = effectiveCap.multiply(BigDecimal.valueOf(holiday.getCapacityPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Count garments due on this date
        long scheduled = 0;
        long completed = 0;
        long pending = 0;
        long cuttingPending = 0;
        long stitchingPending = 0;

        List<Garment> dueGarments = garmentRepository.findAll().stream()
                .filter(g -> date.equals(g.getDueDate()))
                .toList();

        scheduled = dueGarments.size();
        for (Garment g : dueGarments) {
            if (g.getStatus() == GarmentStatus.READY || g.getStatus() == GarmentStatus.DELIVERED) {
                completed++;
            } else {
                pending++;
                if (g.getStatus() == GarmentStatus.FABRIC_CUT) {
                    cuttingPending++;
                } else if (g.getStatus() == GarmentStatus.CUTTING || g.getStatus() == GarmentStatus.STITCHING) {
                    stitchingPending++;
                }
            }
        }

        BigDecimal remainingCapacity = effectiveCap.subtract(BigDecimal.valueOf(pending));
        if (remainingCapacity.compareTo(BigDecimal.ZERO) < 0) {
            remainingCapacity = BigDecimal.ZERO;
        }

        int percentageUsed = 0;
        if (effectiveCap.compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = (int) Math.min(100, Math.round((double) scheduled / effectiveCap.doubleValue() * 100.0));
        }

        // Determine capacity status indicator
        String status;
        if (isBlocked) {
            status = "LOCKED";
        } else if (isHoliday && !deliveryAllowed) {
            status = "HOLIDAY";
        } else if (effectiveCap.compareTo(BigDecimal.ZERO) == 0) {
            status = "RED";
        } else if (pending > effectiveCap.longValue()) {
            status = "RED"; // Over capacity
        } else if (pending >= effectiveCap.longValue() * 0.8) {
            status = "YELLOW"; // Near maximum
        } else {
            status = "GREEN"; // Manageable
        }

        // Group distinct Orders due on this date and compute Order-level dominant phase & delivery progress
        Map<Long, Order> distinctOrders = new LinkedHashMap<>();
        for (Garment g : dueGarments) {
            if (g.getOrder() != null) {
                distinctOrders.putIfAbsent(g.getOrder().getId(), g.getOrder());
            }
        }

        long scheduledOrders = distinctOrders.size();
        long ordersInCutting = 0;
        long ordersInStitching = 0;
        long ordersInIroning = 0;
        long ordersReadyForDelivery = 0;
        long ordersFullyDelivered = 0;
        int totalOrderProgressSum = 0;

        for (Order order : distinctOrders.values()) {
            OrderProductionProgressSummary prog = buildOrderProgress(order);
            totalOrderProgressSum += prog.overallOrderProgress();
            switch (prog.dominantPhase()) {
                case "CUTTING" -> ordersInCutting++;
                case "STITCHING" -> ordersInStitching++;
                case "IRONING" -> ordersInIroning++;
                case "READY_FOR_DELIVERY", "PARTIALLY_DELIVERED" -> ordersReadyForDelivery++;
                case "DELIVERED" -> ordersFullyDelivered++;
            }
        }

        int averageOrderProgress = scheduledOrders > 0
                ? Math.round((float) totalOrderProgressSum / scheduledOrders)
                : 0;

        return new DaySummary(
                date,
                date.getDayOfWeek().getValue(),
                scheduled,
                completed,
                pending,
                cuttingPending,
                stitchingPending,
                maxCap,
                effectiveCap,
                remainingCapacity,
                status,
                percentageUsed,
                isBlocked,
                blockReason,
                isHoliday,
                holidayName,
                deliveryAllowed,
                0,
                0,
                isPowerCut,
                reductionNote,
                scheduledOrders,
                ordersInCutting,
                ordersInStitching,
                ordersInIroning,
                ordersReadyForDelivery,
                ordersFullyDelivered,
                averageOrderProgress
        );
    }

    private ScheduledGarmentSummary toScheduledGarmentSummary(Garment g) {
        return new ScheduledGarmentSummary(
                g.getId(),
                g.getOrder() != null ? g.getOrder().getId() : null,
                g.getOrder() != null && g.getOrder().getCustomer() != null ? g.getOrder().getCustomer().getName() : "Unknown",
                g.getOrder() != null && g.getOrder().getCustomer() != null ? g.getOrder().getCustomer().getPhone() : "",
                g.getService() != null ? g.getService().getName() : "Garment",
                g.getOrder() != null ? g.getOrder().getSampleBookPageNumber() : null,
                g.getStatus() != null ? g.getStatus().name() : "FABRIC_CUT",
                g.getTryDate(),
                g.getDueDate(),
                g.isCustomerFabric(),
                g.getMadeBy() != null ? g.getMadeBy().getId() : null,
                g.getMadeBy() != null ? g.getMadeBy().getName() : null,
                g.getCuttingBy() != null ? g.getCuttingBy().getName() : null
        );
    }

    /**
     * Computes Order-level production progress according to workshop business rules:
     * 1. Strict All-Garments Delivery Rule:
     *    - Order is ONLY "DELIVERED" when ALL active garments in the order are DELIVERED.
     *    - If all garments are ready/delivered (readyCount + deliveredCount == total) and deliveredCount < total,
     *      the order is "PARTIALLY_DELIVERED" (if deliveredCount > 0) or "READY_FOR_DELIVERY" (if deliveredCount == 0),
     *      NEVER "DELIVERED" until every garment is handed over.
     * 2. Dominant Production Phase Comparison Rule:
     *    - When garments are in production phases (Cutting vs Stitching vs Ironing),
     *      compares the count of garments in each phase (e.g., if cuttingCount > ironingCount,
     *      shows CUTTING progress for the order; if ironingCount > cuttingCount, shows IRONING progress).
     */
    private OrderProductionProgressSummary buildOrderProgress(Order order) {
        List<Garment> allOrderGarments = order.getGarments() != null ? order.getGarments() : List.of();
        List<Garment> activeGarments = allOrderGarments.stream()
                .filter(g -> g.getStatus() != GarmentStatus.CANCELLED)
                .toList();
        if (activeGarments.isEmpty()) {
            activeGarments = allOrderGarments;
        }

        int total = activeGarments.size();
        int cuttingCount = 0;
        int stitchingCount = 0;
        int ironingCount = 0;
        int readyCount = 0;
        int deliveredCount = 0;
        int weightedProgressSum = 0;

        for (Garment g : activeGarments) {
            GarmentStatus st = g.getStatus() != null ? g.getStatus() : GarmentStatus.FABRIC_CUT;
            switch (st) {
                case FABRIC_CUT -> {
                    cuttingCount++;
                    weightedProgressSum += 15;
                }
                case CUTTING -> {
                    cuttingCount++;
                    weightedProgressSum += 30;
                }
                case STITCHING -> {
                    stitchingCount++;
                    weightedProgressSum += 55;
                }
                case ALTERATION -> {
                    stitchingCount++;
                    weightedProgressSum += 60;
                }
                case WASHING -> {
                    ironingCount++;
                    weightedProgressSum += 72;
                }
                case IRONING -> {
                    ironingCount++;
                    weightedProgressSum += 82;
                }
                case READY -> {
                    readyCount++;
                    weightedProgressSum += 92;
                }
                case DELIVERED -> {
                    deliveredCount++;
                    weightedProgressSum += 100;
                }
                default -> {
                    cuttingCount++;
                    weightedProgressSum += 10;
                }
            }
        }

        boolean isFullyDelivered = total > 0 && deliveredCount == total;
        boolean isPartiallyDelivered = deliveredCount > 0 && deliveredCount < total;
        boolean isAllReadyOrDelivered = total > 0 && (readyCount + deliveredCount) == total && deliveredCount < total;

        int overallOrderProgress = total > 0 ? Math.round((float) weightedProgressSum / total) : 0;
        // Never allow 100% progress unless ALL garments are DELIVERED
        if (!isFullyDelivered && overallOrderProgress >= 100) {
            overallOrderProgress = 96;
        }

        String dominantPhase;
        String dominantPhaseLabel;
        int dominantPhaseGarmentCount;
        int dominantPhaseProgress;

        if (isFullyDelivered) {
            dominantPhase = "DELIVERED";
            dominantPhaseLabel = "Order Delivered (" + deliveredCount + "/" + total + " Delivered)";
            dominantPhaseGarmentCount = deliveredCount;
            dominantPhaseProgress = 100;
        } else if (isAllReadyOrDelivered) {
            if (deliveredCount > 0) {
                dominantPhase = "PARTIALLY_DELIVERED";
                dominantPhaseLabel = "Ready for Delivery (" + deliveredCount + "/" + total + " Delivered, " + readyCount + " Left)";
                dominantPhaseGarmentCount = readyCount;
                dominantPhaseProgress = Math.round((float) (readyCount + deliveredCount) * 100f / total);
            } else {
                dominantPhase = "READY_FOR_DELIVERY";
                dominantPhaseLabel = "Ready for Delivery (" + readyCount + "/" + total + " Ready)";
                dominantPhaseGarmentCount = readyCount;
                dominantPhaseProgress = 92;
            }
        } else {
            // Compare active production phases (Cutting vs Stitching vs Ironing)
            // If Cutting count >= Stitching count and >= Ironing count -> CUTTING wins
            // If Ironing count > Cutting count and >= Stitching count -> IRONING wins
            // Otherwise -> STITCHING wins
            if (cuttingCount >= stitchingCount && cuttingCount >= ironingCount && cuttingCount > 0) {
                dominantPhase = "CUTTING";
                dominantPhaseGarmentCount = cuttingCount;
                dominantPhaseProgress = Math.round((float) cuttingCount * 100f / total);
                if (ironingCount > 0 || stitchingCount > 0) {
                    dominantPhaseLabel = "Cutting Phase (" + cuttingCount + "/" + total + " in Cutting)";
                } else {
                    dominantPhaseLabel = "Cutting Phase (" + cuttingCount + "/" + total + " Garments)";
                }
            } else if (ironingCount > cuttingCount && ironingCount >= stitchingCount && ironingCount > 0) {
                dominantPhase = "IRONING";
                dominantPhaseGarmentCount = ironingCount;
                dominantPhaseProgress = Math.round((float) ironingCount * 100f / total);
                dominantPhaseLabel = "Ironing Phase (" + ironingCount + "/" + total + " in Ironing)";
            } else {
                dominantPhase = "STITCHING";
                dominantPhaseGarmentCount = stitchingCount;
                dominantPhaseProgress = Math.round((float) stitchingCount * 100f / total);
                dominantPhaseLabel = "Stitching Phase (" + stitchingCount + "/" + total + " in Stitching)";
            }
        }

        List<ScheduledGarmentSummary> orderGarmentSummaries = activeGarments.stream()
                .map(this::toScheduledGarmentSummary)
                .toList();

        return new OrderProductionProgressSummary(
                order.getId(),
                order.getCustomer() != null ? order.getCustomer().getName() : "Unknown",
                order.getCustomer() != null ? order.getCustomer().getPhone() : "",
                order.getSampleBookPageNumber(),
                order.getOrderDate(),
                total,
                cuttingCount,
                stitchingCount,
                ironingCount,
                readyCount,
                deliveredCount,
                dominantPhase,
                dominantPhaseLabel,
                dominantPhaseGarmentCount,
                dominantPhaseProgress,
                overallOrderProgress,
                isFullyDelivered,
                isPartiallyDelivered,
                isAllReadyOrDelivered,
                orderGarmentSummaries
        );
    }
}
