package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.worker.WorkerDtos.*;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.entities.WorkerAttendance.AttendanceStatus;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.GarmentRepository;
import com.sff.authapp.login.repositories.RoleRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.repositories.WorkerAttendanceRepository;
import com.sff.authapp.login.services.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerServiceImpl implements WorkerService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WorkerAttendanceRepository attendanceRepository;
    private final GarmentRepository garmentRepository;
    private final com.sff.authapp.login.repositories.WorkerAdvanceRepository workerAdvanceRepository;
    private final com.sff.authapp.login.repositories.WorkerSettlementRepository workerSettlementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponseDto> getAllWorkers(String keyword) {
        List<User> workers;
        if (keyword != null && !keyword.isBlank()) {
            workers = userRepository.searchWorkersList(keyword.trim());
        } else {
            workers = userRepository.findAllWorkersList();
        }

        LocalDate today = LocalDate.now();
        Map<Long, WorkerAttendance> todayAttendanceMap = attendanceRepository
                .findByAttendanceDateOrderByWorkerIdAsc(today)
                .stream()
                .filter(wa -> wa.getWorker() != null)
                .collect(Collectors.toMap(
                        wa -> wa.getWorker().getId(),
                        wa -> wa,
                        (a, b) -> a
                ));

        return workers.stream()
                .map(w -> toResponse(w, todayAttendanceMap.get(w.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponseDto getWorkerById(Long workerId) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + workerId));
        WorkerAttendance todayAtt = attendanceRepository
                .findByWorkerIdAndAttendanceDate(workerId, LocalDate.now())
                .orElse(null);
        return toResponse(worker, todayAtt);
    }

    @Override
    public WorkerResponseDto createWorker(WorkerRequestDto request) {
        Role workerRole = roleRepository.findByName("ROLE_WORKER")
                .or(() -> roleRepository.findByName("WORKER"))
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_WORKER").build()));

        Set<Role> roles = new HashSet<>();
        roles.add(workerRole);

        BigDecimal cap = request.dailyCapacity() != null
                ? request.dailyCapacity()
                : new BigDecimal("5.00");

        String specialty = (request.workerSpecialty() != null && !request.workerSpecialty().isBlank())
                ? request.workerSpecialty().trim().toUpperCase()
                : "STITCHING";

        User worker = User.builder()
                .name(request.name().trim())
                .phone(request.phone().trim())
                .alternatePhone(request.alternatePhone() != null ? request.alternatePhone().trim() : null)
                .dailyCapacity(cap)
                .workerSpecialty(specialty)
                .image(request.imageUrl() != null && !request.imageUrl().isBlank() ? request.imageUrl().trim() : null)
                .imagePublicId(request.imagePublicId() != null && !request.imagePublicId().isBlank() ? request.imagePublicId().trim() : null)
                .address(request.address() != null ? request.address().trim() : null)
                .city(request.city() != null ? request.city().trim() : null)
                .notes(request.notes() != null ? request.notes().trim() : null)
                .enable(request.active() == null || request.active())
                .provider(Provider.LOCAL)
                .roles(roles)
                .build();

        User saved = userRepository.save(worker);
        return toResponse(saved, null);
    }

    @Override
    public WorkerResponseDto updateWorker(Long workerId, WorkerRequestDto request) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + workerId));

        if (request.name() != null && !request.name().isBlank()) {
            worker.setName(request.name().trim());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            worker.setPhone(request.phone().trim());
        }
        worker.setAlternatePhone(request.alternatePhone() != null ? request.alternatePhone().trim() : null);

        if (request.dailyCapacity() != null) {
            worker.setDailyCapacity(request.dailyCapacity());
        }
        if (request.workerSpecialty() != null && !request.workerSpecialty().isBlank()) {
            worker.setWorkerSpecialty(request.workerSpecialty().trim().toUpperCase());
        }

        worker.setImage(request.imageUrl() != null && !request.imageUrl().isBlank() ? request.imageUrl().trim() : null);
        worker.setImagePublicId(request.imagePublicId() != null && !request.imagePublicId().isBlank() ? request.imagePublicId().trim() : null);
        worker.setAddress(request.address() != null ? request.address().trim() : null);
        worker.setCity(request.city() != null ? request.city().trim() : null);
        worker.setNotes(request.notes() != null ? request.notes().trim() : null);

        if (request.active() != null) {
            worker.setEnable(request.active());
        }

        User saved = userRepository.save(worker);
        WorkerAttendance todayAtt = attendanceRepository
                .findByWorkerIdAndAttendanceDate(workerId, LocalDate.now())
                .orElse(null);
        return toResponse(saved, todayAtt);
    }

    @Override
    public WorkerResponseDto updateWorkerCapacity(Long workerId, BigDecimal dailyCapacity) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + workerId));
        worker.setDailyCapacity(dailyCapacity != null ? dailyCapacity : new BigDecimal("5.00"));
        User saved = userRepository.save(worker);
        WorkerAttendance todayAtt = attendanceRepository
                .findByWorkerIdAndAttendanceDate(workerId, LocalDate.now())
                .orElse(null);
        return toResponse(saved, todayAtt);
    }

    @Override
    public WorkerResponseDto updateWorkerStatus(Long workerId, boolean active) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + workerId));
        worker.setEnable(active);
        User saved = userRepository.save(worker);
        WorkerAttendance todayAtt = attendanceRepository
                .findByWorkerIdAndAttendanceDate(workerId, LocalDate.now())
                .orElse(null);
        return toResponse(saved, todayAtt);
    }

    @Override
    public void deleteWorker(Long workerId) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + workerId));
        List<WorkerAttendance> att = attendanceRepository.findByWorkerIdOrderByAttendanceDateDesc(workerId);
        if (att != null && !att.isEmpty()) {
            attendanceRepository.deleteAll(att);
        }
        userRepository.delete(worker);
    }

    // ============================================================
    // WORK ALLOCATION & READY FOR DELIVERY CHECK
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<WorkAllocationItemDto> getWorkAllocations(Long workerId, String status) {
        return getWorkAllocations(workerId, status, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkAllocationItemDto> getWorkAllocations(
            Long workerId,
            String status,
            Long serviceId,
            LocalDate startDate,
            LocalDate endDate,
            String stage
    ) {
        List<Garment> allGarments = garmentRepository.findAll().stream()
                .filter(g -> g.getStatus() != GarmentStatus.CANCELLED)
                .sorted(Comparator.comparing(
                        (Garment g) -> g.getDueDate() != null ? g.getDueDate() : LocalDate.MAX
                ).thenComparing(Garment::getId))
                .toList();

        // Precompute garment index & total count per order
        Map<Long, List<Long>> orderGarmentIdsMap = new HashMap<>();
        for (Garment g : allGarments) {
            if (g.getOrder() != null) {
                orderGarmentIdsMap
                        .computeIfAbsent(g.getOrder().getId(), k -> new ArrayList<>())
                        .add(g.getId());
            }
        }
        for (List<Long> ids : orderGarmentIdsMap.values()) {
            Collections.sort(ids);
        }

        return allGarments.stream()
                // Stage filtering
                .filter(g -> {
                    if (stage == null || stage.isBlank() || stage.equalsIgnoreCase("ALL")) {
                        return true;
                    }
                    if (stage.equalsIgnoreCase("PENDING_ALLOTMENT")) {
                        // Must be cut by master (CUTTING status or cuttingBy != null) AND not yet allotted to worker AND not FABRIC_CUT
                        return (g.getStatus() == GarmentStatus.CUTTING || g.getCuttingBy() != null)
                                && g.getMadeBy() == null
                                && g.getStatus() != GarmentStatus.FABRIC_CUT;
                    }
                    if (stage.equalsIgnoreCase("READY_QUEUE")) {
                        // Garments allocated to worker and active in shop (not READY or DELIVERED)
                        return g.getMadeBy() != null
                                && g.getStatus() != GarmentStatus.READY
                                && g.getStatus() != GarmentStatus.DELIVERED;
                    }
                    if (stage.equalsIgnoreCase("HISTORY")) {
                        // Garments that have a worker assigned (production history)
                        return g.getMadeBy() != null;
                    }
                    return true;
                })
                // Worker filter
                .filter(g -> {
                    if (workerId != null) {
                        return (g.getMadeBy() != null && Objects.equals(g.getMadeBy().getId(), workerId))
                                || (g.getCuttingBy() != null && Objects.equals(g.getCuttingBy().getId(), workerId));
                    }
                    return true;
                })
                // Status filter
                .filter(g -> {
                    if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                        return g.getStatus() != null && g.getStatus().name().equalsIgnoreCase(status.trim());
                    }
                    return true;
                })
                // Service filter
                .filter(g -> {
                    if (serviceId != null) {
                        return g.getService() != null && Objects.equals(g.getService().getId(), serviceId);
                    }
                    return true;
                })
                // Date range filter
                .filter(g -> {
                    if (startDate == null && endDate == null) {
                        return true;
                    }
                    LocalDate garmentDate = g.getOrder() != null && g.getOrder().getOrderDate() != null
                            ? g.getOrder().getOrderDate()
                            : (g.getCreatedAt() != null ? g.getCreatedAt().toLocalDate() : null);
                    if (garmentDate == null) {
                        return false;
                    }
                    if (startDate != null && garmentDate.isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && garmentDate.isAfter(endDate)) {
                        return false;
                    }
                    return true;
                })
                .map(g -> toWorkAllocationDto(g, orderGarmentIdsMap))
                .toList();
    }

    @Override
    public List<WorkAllocationItemDto> allocateWorkToWorker(AllocateWorkRequestDto request) {
        User worker = userRepository.findById(request.workerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + request.workerId()));

        List<Garment> updatedGarments = new ArrayList<>();
        for (Long garmentId : request.garmentIds()) {
            Garment garment = garmentRepository.findById(garmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + garmentId));

            // Set "madeBy" worker on the garment and transition status to STITCHING
            garment.setMadeBy(worker);
            garment.setStatus(GarmentStatus.STITCHING);

            if (request.note() != null && !request.note().isBlank()) {
                garment.setNote(request.note().trim());
            }

            updatedGarments.add(garmentRepository.save(garment));
        }

        // Compute order garment maps for response
        Map<Long, List<Long>> orderGarmentIdsMap = new HashMap<>();
        for (Garment g : updatedGarments) {
            if (g.getOrder() != null) {
                Long oid = g.getOrder().getId();
                if (!orderGarmentIdsMap.containsKey(oid)) {
                    List<Long> ids = garmentRepository.findByOrderIdOrderByIdAsc(oid)
                            .stream()
                            .filter(og -> og.getStatus() != GarmentStatus.CANCELLED)
                            .map(Garment::getId)
                            .sorted()
                            .toList();
                    orderGarmentIdsMap.put(oid, ids);
                }
            }
        }

        return updatedGarments.stream()
                .map(g -> toWorkAllocationDto(g, orderGarmentIdsMap))
                .toList();
    }

    @Override
    public WorkAllocationItemDto markAllocatedGarmentReady(Long garmentId, boolean ready) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + garmentId));

        if (ready) {
            garment.setStatus(GarmentStatus.READY);
        } else {
            garment.setStatus(GarmentStatus.STITCHING);
        }

        Garment saved = garmentRepository.save(garment);

        Map<Long, List<Long>> orderGarmentIdsMap = new HashMap<>();
        if (saved.getOrder() != null) {
            Long oid = saved.getOrder().getId();
            List<Long> ids = garmentRepository.findByOrderIdOrderByIdAsc(oid)
                    .stream()
                    .filter(og -> og.getStatus() != GarmentStatus.CANCELLED)
                    .map(Garment::getId)
                    .sorted()
                    .toList();
            orderGarmentIdsMap.put(oid, ids);
        }

        return toWorkAllocationDto(saved, orderGarmentIdsMap);
    }

    private WorkAllocationItemDto toWorkAllocationDto(
            Garment g,
            Map<Long, List<Long>> orderGarmentIdsMap
    ) {
        Order order = g.getOrder();
        Long orderId = order != null ? order.getId() : null;
        List<Long> siblingIds = orderId != null
                ? orderGarmentIdsMap.getOrDefault(orderId, List.of(g.getId()))
                : List.of(g.getId());

        int idx = siblingIds.indexOf(g.getId());
        int garmentIndexInOrder = idx >= 0 ? idx + 1 : 1;
        int totalGarmentsInOrder = Math.max(siblingIds.size(), 1);

        User customer = order != null ? order.getCustomer() : null;
        User cuttingBy = g.getCuttingBy();
        User madeBy = g.getMadeBy();

        List<AllocationAddOnDto> addOnDtos = g.getAddOns() != null
                ? g.getAddOns().stream()
                .map(a -> {
                    BigDecimal comm = a.getTailorCommission();
                    if (comm == null && a.getAddOn() != null) {
                        comm = a.getAddOn().getTailorCommission();
                    }
                    return new AllocationAddOnDto(
                            a.getId(),
                            a.getAddOnName(),
                            a.getQuantity(),
                            a.getAddOn() != null ? a.getAddOn().getImageUrl() : null,
                            a.getNote(),
                            comm
                    );
                })
                .toList()
                : List.of();

        return new WorkAllocationItemDto(
                g.getId(),
                orderId,
                order != null ? order.getOrderDate() : null,
                order != null ? order.getSampleBookPageNumber() : null,
                garmentIndexInOrder,
                totalGarmentsInOrder,
                customer != null ? customer.getName() : "Customer",
                customer != null ? customer.getPhone() : "",
                g.getService() != null ? g.getService().getId() : null,
                g.getService() != null ? g.getService().getName() : "Garment",
                g.getSampleNumber(),
                g.getStatus(),
                g.isCustomerFabric(),
                g.getTryDate(),
                g.getDueDate(),
                g.getNote(),
                cuttingBy != null ? cuttingBy.getId() : null,
                cuttingBy != null ? cuttingBy.getName() : null,
                madeBy != null ? madeBy.getId() : null,
                madeBy != null ? madeBy.getName() : null,
                madeBy != null ? madeBy.getPhone() : null,
                madeBy != null ? madeBy.getImage() : null,
                madeBy != null ? madeBy.getWorkerSpecialty() : null,
                addOnDtos,
                g.getPrice(),
                g.getService() != null ? g.getService().getTailorCommission() : null,
                g.getUpdatedAt(),
                g.getCreatedAt()
        );
    }

    // ============================================================
    // WORKER ADVANCES & SETTLEMENTS (HISAB)
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<WorkerAdvanceDto> getWorkerAdvances(Long workerId, LocalDate startDate, LocalDate endDate) {
        List<WorkerAdvance> advances;
        if (startDate != null && endDate != null) {
            advances = workerAdvanceRepository.findByWorkerIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(workerId, startDate, endDate);
        } else {
            advances = workerAdvanceRepository.findByWorkerIdOrderByAdvanceDateDesc(workerId);
        }
        return advances.stream()
                .map(this::toAdvanceDto)
                .toList();
    }

    @Override
    public WorkerAdvanceDto createWorkerAdvance(CreateWorkerAdvanceRequestDto request) {
        User worker = userRepository.findById(request.workerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + request.workerId()));

        WorkerAdvance advance = WorkerAdvance.builder()
                .worker(worker)
                .amount(request.amount())
                .advanceDate(request.advanceDate())
                .type(request.type() != null && !request.type().isBlank() ? request.type().trim() : "DAILY_KHARCHA")
                .note(request.note() != null ? request.note().trim() : null)
                .paymentMode(request.paymentMode() != null && !request.paymentMode().isBlank() ? request.paymentMode().trim() : "CASH")
                .build();

        WorkerAdvance saved = workerAdvanceRepository.save(advance);
        return toAdvanceDto(saved);
    }

    @Override
    public void deleteWorkerAdvance(Long advanceId) {
        if (!workerAdvanceRepository.existsById(advanceId)) {
            throw new ResourceNotFoundException("Worker advance not found with id: " + advanceId);
        }
        workerAdvanceRepository.deleteById(advanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerSettlementDto> getWorkerSettlements(Long workerId) {
        return workerSettlementRepository.findByWorkerIdOrderBySettlementDateDesc(workerId).stream()
                .map(this::toSettlementDto)
                .toList();
    }

    @Override
    public WorkerSettlementDto createWorkerSettlement(CreateWorkerSettlementRequestDto request) {
        User worker = userRepository.findById(request.workerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + request.workerId()));

        String stmtNum = "STMT-" + System.currentTimeMillis();

        WorkerSettlement settlement = WorkerSettlement.builder()
                .settlementNumber(stmtNum)
                .worker(worker)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .settlementDate(request.settlementDate())
                .cycleLabel(request.cycleLabel())
                .totalGarmentsCount(request.totalGarmentsCount())
                .grossWorkAmount(request.grossWorkAmount())
                .previousBalance(request.previousBalance())
                .totalAdvancesDeducted(request.totalAdvancesDeducted())
                .netPayable(request.netPayable())
                .amountPaid(request.amountPaid())
                .closingBalance(request.closingBalance())
                .paymentMode(request.paymentMode())
                .note(request.note())
                .garmentBreakdownJson(request.garmentBreakdownJson())
                .build();

        WorkerSettlement saved = workerSettlementRepository.save(settlement);
        return toSettlementDto(saved);
    }

    private WorkerAdvanceDto toAdvanceDto(WorkerAdvance a) {
        return new WorkerAdvanceDto(
                a.getId(),
                a.getWorker() != null ? a.getWorker().getId() : null,
                a.getWorker() != null ? a.getWorker().getName() : null,
                a.getAmount(),
                a.getAdvanceDate(),
                a.getType(),
                a.getNote(),
                a.getPaymentMode(),
                a.getSettlementId(),
                a.getCreatedAt()
        );
    }

    private WorkerSettlementDto toSettlementDto(WorkerSettlement s) {
        return new WorkerSettlementDto(
                s.getId(),
                s.getSettlementNumber(),
                s.getWorker() != null ? s.getWorker().getId() : null,
                s.getWorker() != null ? s.getWorker().getName() : null,
                s.getStartDate(),
                s.getEndDate(),
                s.getSettlementDate(),
                s.getCycleLabel(),
                s.getTotalGarmentsCount(),
                s.getGrossWorkAmount(),
                s.getPreviousBalance(),
                s.getTotalAdvancesDeducted(),
                s.getNetPayable(),
                s.getAmountPaid(),
                s.getClosingBalance(),
                s.getPaymentMode(),
                s.getNote(),
                s.getGarmentBreakdownJson(),
                s.getCreatedAt()
        );
    }

    private WorkerResponseDto toResponse(User worker, WorkerAttendance todayAtt) {
        AttendanceStatus attStatus = todayAtt != null ? todayAtt.getStatus() : AttendanceStatus.PRESENT;
        int attPct = todayAtt != null ? todayAtt.getCapacityPercentage() : 100;

        return new WorkerResponseDto(
                worker.getId(),
                worker.getName(),
                worker.getPhone(),
                worker.getAlternatePhone(),
                worker.getDailyCapacity() != null ? worker.getDailyCapacity() : new BigDecimal("5.00"),
                worker.getWorkerSpecialty() != null ? worker.getWorkerSpecialty() : "STITCHING",
                worker.getImage(),
                worker.getImagePublicId(),
                worker.getAddress(),
                worker.getCity(),
                worker.getNotes(),
                worker.isEnable(),
                attStatus,
                attPct,
                worker.getCreatedAt()
        );
    }
}
