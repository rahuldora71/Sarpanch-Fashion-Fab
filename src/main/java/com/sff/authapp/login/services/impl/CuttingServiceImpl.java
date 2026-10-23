package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.cutting.*;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.services.CuttingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CuttingServiceImpl implements CuttingService {

    private final CuttingQueueRepository cuttingQueueRepository;
    private final GarmentRepository garmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FieldRepository fieldRepository;

    private static final DateTimeFormatter MONTH_DAY_FMT = DateTimeFormatter.ofPattern("d MMM");

    // ============================================================
    // GET CUTTING QUEUE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CuttingQueueResponse getCuttingQueue() {
        // In the existing enum:
        // Initial status of garment on creation is FABRIC_CUT (representing "pending cutting").
        // We find all garments with FABRIC_CUT.
        List<Garment> pendingGarments =
                cuttingQueueRepository.findGarmentsForCuttingQueue(GarmentStatus.FABRIC_CUT);

        return buildQueueResponse(pendingGarments);
    }

    @Override
    @Transactional(readOnly = true)
    public CuttingQueueResponse getCuttingQueueByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Garment> pendingGarments =
                cuttingQueueRepository.findGarmentsForCuttingQueueByDateRange(
                        GarmentStatus.FABRIC_CUT, startDate, endDate
                );

        return buildQueueResponse(pendingGarments);
    }

    // ============================================================
    // GET CUTTING WORKSPACE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CuttingWorkspaceResponse getCuttingWorkspace(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User customer = order.getCustomer();
        CuttingQueueResponse.CustomerInfo customerInfo = new CuttingQueueResponse.CustomerInfo(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getName(),
                customer.getPhone(),
                customer.getImage()
        );

        List<Garment> allGarments =
                cuttingQueueRepository.findGarmentsWithFullDetailsByOrderId(orderId);

        List<CuttingWorkspaceResponse.CuttingWorkspaceGarment> workspaceGarments =
                allGarments.stream()
                        .map(this::mapToWorkspaceGarment)
                        .toList();

        return new CuttingWorkspaceResponse(
                order.getId(),
                order.getSampleBookPageNumber(),
                order.getOrderDate(),
                customerInfo,
                workspaceGarments,
                LocalDateTime.now()
        );
    }

    // ============================================================
    // MARK AS CUT
    // ============================================================

    @Override
    public GarmentResponse markAsCut(Long garmentId, MarkAsCutRequest request, Long authenticatedUserId) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + garmentId));

        // Authenticated user takes precedence for cutter audit field
        Long cutterId = authenticatedUserId;
        if (cutterId == null && request != null && request.cuttingByUserId() != null) {
            cutterId = request.cuttingByUserId();
        }

        User cutter = null;
        if (cutterId != null) {
            cutter = userRepository.findById(cutterId).orElse(null);
        }

        // Update status to CUTTING or STITCHING (transitions from FABRIC_CUT to CUTTING)
        garment.setStatus(GarmentStatus.CUTTING);
        if (cutter != null) {
            garment.setCuttingBy(cutter);
        }

        // If cutter left a note or fabric marking note, append or update notes
        if (request != null) {
            StringBuilder noteBuilder = new StringBuilder();
            if (garment.getNote() != null && !garment.getNote().isBlank()) {
                noteBuilder.append(garment.getNote());
            }

            if (request.fabricMarkingConfirmation() != null && !request.fabricMarkingConfirmation().isBlank()) {
                if (!noteBuilder.isEmpty()) noteBuilder.append(" | ");
                noteBuilder.append("Fabric Marking: ").append(request.fabricMarkingConfirmation());
            }

            if (request.cuttingNote() != null && !request.cuttingNote().isBlank()) {
                if (!noteBuilder.isEmpty()) noteBuilder.append(" | ");
                noteBuilder.append("Cutting Note: ").append(request.cuttingNote());
            }

            if (!noteBuilder.isEmpty()) {
                garment.setNote(noteBuilder.toString());
            }
        }

        Garment savedGarment = garmentRepository.save(garment);

        return mapToGarmentResponse(savedGarment);
    }

    @Override
    public GarmentResponse cancelCut(Long garmentId, Long authenticatedUserId) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + garmentId));

        // Can only revert if currently in CUTTING status (has not proceeded further to STITCHING/WASHING etc.)
        if (garment.getStatus() != GarmentStatus.CUTTING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot revert cut status because garment has already progressed to " + garment.getStatus()
            );
        }

        garment.setStatus(GarmentStatus.FABRIC_CUT);
        garment.setCuttingBy(null);

        Garment savedGarment = garmentRepository.save(garment);
        return mapToGarmentResponse(savedGarment);
    }

    // ============================================================
    // HELPER BUILDERS
    // ============================================================

    private CuttingQueueResponse buildQueueResponse(List<Garment> pendingGarments) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Group garments by dueDate
        // (Null due dates go to a far future synthetic date for sorting at end, or handled explicitly)
        Map<LocalDate, List<Garment>> garmentsByDate = new TreeMap<>(
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        for (Garment g : pendingGarments) {
            LocalDate d = g.getDueDate() != null ? g.getDueDate() : LocalDate.of(2099, 12, 31);
            garmentsByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(g);
        }

        List<CuttingQueueResponse.CuttingDateGroup> dateGroups = new ArrayList<>();
        int totalOrdersCount = 0;
        Set<Long> uniqueOrders = new HashSet<>();

        for (Map.Entry<LocalDate, List<Garment>> entry : garmentsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Garment> garmentsOnDate = entry.getValue();

            boolean isOverdue = date.isBefore(today);
            boolean isToday = date.equals(today);
            boolean isTomorrow = date.equals(tomorrow);

            String label;
            if (isToday) {
                label = "Today (" + date.format(MONTH_DAY_FMT) + ")";
            } else if (isTomorrow) {
                label = "Tomorrow (" + date.format(MONTH_DAY_FMT) + ")";
            } else if (isOverdue) {
                label = "Overdue (" + date.format(MONTH_DAY_FMT) + ")";
            } else if (date.getYear() == 2099) {
                label = "No Delivery Date Set";
            } else {
                label = date.format(MONTH_DAY_FMT);
            }

            // Group by Order within this date
            Map<Long, List<Garment>> garmentsByOrder = new LinkedHashMap<>();
            for (Garment g : garmentsOnDate) {
                garmentsByOrder.computeIfAbsent(g.getOrder().getId(), k -> new ArrayList<>()).add(g);
                uniqueOrders.add(g.getOrder().getId());
            }

            List<CuttingQueueResponse.CuttingOrderGroup> orderGroups = new ArrayList<>();
            for (Map.Entry<Long, List<Garment>> orderEntry : garmentsByOrder.entrySet()) {
                List<Garment> orderGarments = orderEntry.getValue();
                Garment first = orderGarments.get(0);
                Order order = first.getOrder();
                User customer = order.getCustomer();

                CuttingQueueResponse.CustomerInfo customerInfo = new CuttingQueueResponse.CustomerInfo(
                        customer.getId(),
                        customer.getCustomerCode(),
                        customer.getName(),
                        customer.getPhone(),
                        customer.getImage()
                );

                List<CuttingQueueResponse.CuttingGarmentItem> garmentItems = orderGarments.stream()
                        .map(this::mapToGarmentItem)
                        .toList();

                orderGroups.add(new CuttingQueueResponse.CuttingOrderGroup(
                        order.getId(),
                        order.getSampleBookPageNumber(),
                        order.getOrderDate(),
                        customerInfo,
                        garmentItems.size(),
                        garmentItems
                ));
            }

            dateGroups.add(new CuttingQueueResponse.CuttingDateGroup(
                    date.getYear() == 2099 ? null : date,
                    label,
                    isOverdue,
                    isToday,
                    isTomorrow,
                    garmentsOnDate.size(),
                    orderGroups
            ));
        }

        return new CuttingQueueResponse(
                dateGroups,
                pendingGarments.size(),
                uniqueOrders.size(),
                today
        );
    }

    private CuttingQueueResponse.CuttingGarmentItem mapToGarmentItem(Garment g) {
        // Measurement
        CuttingQueueResponse.MeasurementInfo measurementInfo = null;
        if (g.getGarmentMeasurement() != null) {
            GarmentMeasurement gm = g.getGarmentMeasurement();
            measurementInfo = new CuttingQueueResponse.MeasurementInfo(
                    gm.getId(),
                    resolveMeasurementFieldNames(gm),
                    gm.getNote(),
                    gm.getTakenBy() != null ? gm.getTakenBy().getId() : null,
                    gm.getCreatedAt()
            );
        }

        // Split Add-ons into cutting vs stitching
        List<CuttingQueueResponse.AddOnInfo> cuttingAddOns = new ArrayList<>();
        List<CuttingQueueResponse.AddOnInfo> stitchingAddOns = new ArrayList<>();

        if (g.getAddOns() != null) {
            for (GarmentAddOn ga : g.getAddOns()) {
                AddOn a = ga.getAddOn();
                boolean isBeforeCutting = a == null || a.isBeforeCutting();

                String cuttingInstruction = generateCuttingInstruction(a != null ? a.getName() : "Add-on");
                String fabricMarkingNote = generateFabricMarkingNote(a != null ? a.getName() : "Add-on");

                CuttingQueueResponse.AddOnInfo info = new CuttingQueueResponse.AddOnInfo(
                        ga.getId(),
                        a != null ? a.getId() : null,
                        a != null ? a.getName() : "Add-on",
                        a != null ? a.getImageUrl() : null,
                        ga.getQuantity(),
                        ga.getUnitPrice(),
                        ga.getTotalPrice(),
                        isBeforeCutting,
                        cuttingInstruction,
                        fabricMarkingNote,
                        ga.getNote()
                );

                if (isBeforeCutting) {
                    cuttingAddOns.add(info);
                } else {
                    stitchingAddOns.add(info);
                }
            }
        }

        return new CuttingQueueResponse.CuttingGarmentItem(
                g.getId(),
                g.getService().getId(),
                g.getService().getName(),
                g.getPrice(),
                g.getSampleNumber(),
                g.getStatus(),
                g.isCustomerFabric(),
                g.getDueDate(),
                g.getTryDate(),
                g.getNote(),
                measurementInfo,
                cuttingAddOns,
                stitchingAddOns,
                g.getCuttingBy() != null ? g.getCuttingBy().getId() : null,
                g.getCuttingBy() != null ? g.getCuttingBy().getName() : null,
                g.getCreatedAt()
        );
    }

    private CuttingWorkspaceResponse.CuttingWorkspaceGarment mapToWorkspaceGarment(Garment g) {
        boolean isPendingCutting = (g.getStatus() == GarmentStatus.FABRIC_CUT);

        CuttingQueueResponse.MeasurementInfo measurementInfo = null;
        if (g.getGarmentMeasurement() != null) {
            GarmentMeasurement gm = g.getGarmentMeasurement();
            measurementInfo = new CuttingQueueResponse.MeasurementInfo(
                    gm.getId(),
                    resolveMeasurementFieldNames(gm),
                    gm.getNote(),
                    gm.getTakenBy() != null ? gm.getTakenBy().getId() : null,
                    gm.getCreatedAt()
            );
        }

        List<CuttingQueueResponse.AddOnInfo> cuttingAddOns = new ArrayList<>();
        List<CuttingQueueResponse.AddOnInfo> otherAddOns = new ArrayList<>();

        if (g.getAddOns() != null) {
            for (GarmentAddOn ga : g.getAddOns()) {
                AddOn a = ga.getAddOn();
                boolean isBeforeCutting = a == null || a.isBeforeCutting();

                String cuttingInstruction = generateCuttingInstruction(a != null ? a.getName() : "Add-on");
                String fabricMarkingNote = generateFabricMarkingNote(a != null ? a.getName() : "Add-on");

                CuttingQueueResponse.AddOnInfo info = new CuttingQueueResponse.AddOnInfo(
                        ga.getId(),
                        a != null ? a.getId() : null,
                        a != null ? a.getName() : "Add-on",
                        a != null ? a.getImageUrl() : null,
                        ga.getQuantity(),
                        ga.getUnitPrice(),
                        ga.getTotalPrice(),
                        isBeforeCutting,
                        cuttingInstruction,
                        fabricMarkingNote,
                        ga.getNote()
                );

                if (isBeforeCutting) {
                    cuttingAddOns.add(info);
                } else {
                    otherAddOns.add(info);
                }
            }
        }

        return new CuttingWorkspaceResponse.CuttingWorkspaceGarment(
                g.getId(),
                g.getService().getId(),
                g.getService().getName(),
                g.getPrice(),
                g.getSampleNumber(),
                g.getStatus(),
                g.isCustomerFabric(),
                g.getDueDate(),
                g.getTryDate(),
                g.getNote(),
                isPendingCutting,
                measurementInfo,
                cuttingAddOns,
                otherAddOns,
                g.getCuttingBy() != null ? g.getCuttingBy().getId() : null,
                g.getCuttingBy() != null ? g.getCuttingBy().getName() : null,
                g.getStatus() != GarmentStatus.FABRIC_CUT ? g.getUpdatedAt() : null
        );
    }

    private String generateCuttingInstruction(String addOnName) {
        String lower = addOnName.toLowerCase();
        if (lower.contains("pocket")) {
            return "Cut additional pocket piece with exact pattern matching.";
        } else if (lower.contains("collar")) {
            return "Follow special collar cutting pattern & fused collar canvas.";
        } else if (lower.contains("cuff") || lower.contains("sleeve")) {
            return "Cut custom cuff width and sleeve placket allowances.";
        } else if (lower.contains("lining") || lower.contains("inner")) {
            return "Cut complete inner lining fabric matching outer shell pattern.";
        } else if (lower.contains("pleat") || lower.contains("dart")) {
            return "Include extended fabric allowance for special pleats/darts.";
        }
        return "Check pattern allowance before cutting.";
    }

    private String generateFabricMarkingNote(String addOnName) {
        String lower = addOnName.toLowerCase();
        if (lower.contains("pocket")) {
            return "Mark pocket placement on left chest/side.";
        } else if (lower.contains("collar")) {
            return "Mark center back notch and collar point alignment.";
        } else if (lower.contains("pleat")) {
            return "Mark pleat fold lines and dart points clearly with tailor chalk.";
        }
        return "Mark alignment notches for stitching assembly.";
    }

    private GarmentResponse mapToGarmentResponse(Garment garment) {
        Long measurementId = null;
        if (garment.getGarmentMeasurement() != null) {
            measurementId = garment.getGarmentMeasurement().getId();
        }

        Long cuttingById = garment.getCuttingBy() != null ? garment.getCuttingBy().getId() : null;
        Long madeById = garment.getMadeBy() != null ? garment.getMadeBy().getId() : null;
        Long deliveredById = garment.getDeliveredBy() != null ? garment.getDeliveredBy().getId() : null;

        return new GarmentResponse(
                garment.getId(),
                garment.getOrder().getId(),
                garment.getService().getId(),
                garment.getService().getName(),
                garment.getPrice(),
                garment.getSampleNumber(),
                measurementId,
                garment.getStatus(),
                garment.isCustomerFabric(),
                garment.getTryDate(),
                garment.getDueDate(),
                cuttingById,
                madeById,
                deliveredById,
                garment.getAlterationDeliveryDate(),
                garment.getAlterationNote(),
                garment.getNote(),
                garment.getCreatedAt(),
                garment.getUpdatedAt()
        );
    }

    private Map<String, Object> resolveMeasurementFieldNames(GarmentMeasurement gm) {
        if (gm == null || gm.getMeasurementFields() == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> raw = gm.getMeasurementFields();
        Map<Long, String> fieldIdToName = fieldRepository.findAll().stream()
                .collect(Collectors.toMap(Field::getId, Field::getName, (existing, replacement) -> existing));

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            try {
                Long id = Long.parseLong(key);
                String fieldName = fieldIdToName.get(id);
                resolved.put(fieldName != null ? fieldName : key, entry.getValue());
            } catch (NumberFormatException e) {
                resolved.put(key, entry.getValue());
            }
        }
        return resolved;
    }
}
