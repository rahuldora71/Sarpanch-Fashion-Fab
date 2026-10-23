package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.alteration.AlterationBatchRequestDto;
import com.sff.authapp.login.dtos.alteration.AlterationBatchResponseDto;
import com.sff.authapp.login.dtos.alteration.AlterationQueueItemDto;
import com.sff.authapp.login.dtos.alteration.AlterationQueueSummaryDto;
import com.sff.authapp.login.dtos.alteration.GarmentAlterationItemDto;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.services.AlterationService;
import com.sff.authapp.login.services.GarmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AlterationServiceImpl implements AlterationService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final GarmentRepository garmentRepository;
    private final GarmentMeasurementRepository garmentMeasurementRepository;
    private final TailoringServiceRepository tailoringServiceRepository;
    private final GarmentService garmentService;

    @Override
    public AlterationBatchResponseDto processAlterations(AlterationBatchRequestDto request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));

        User customer = userRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.customerId()));

        User currentUser = getCurrentUser();

        List<Long> alteredGarmentIds = new ArrayList<>();
        Map<Long, GarmentAlterationItemDto> latestAlterationPerService = new HashMap<>();

        for (GarmentAlterationItemDto item : request.garmentAlterations()) {
            Garment garment = garmentRepository.findById(item.garmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + item.garmentId()));

            if (!Objects.equals(garment.getOrder().getId(), order.getId())) {
                throw new IllegalArgumentException("Garment #" + item.garmentId() + " does not belong to Order #" + order.getId());
            }

            // 1. Update garment status & alteration details
            garment.setStatus(GarmentStatus.ALTERATION);

            String altNote = item.alterationNote() != null && !item.alterationNote().trim().isEmpty()
                    ? item.alterationNote().trim()
                    : request.overallNote();
            garment.setAlterationNote(altNote);

            if (item.alterationDeliveryDate() != null) {
                garment.setAlterationDeliveryDate(item.alterationDeliveryDate());
            } else if (request.deliveryDate() != null) {
                garment.setAlterationDeliveryDate(request.deliveryDate());
            }

            // 2. Update garment measurement
            GarmentMeasurement existingMeasurement = garment.getGarmentMeasurement();
            Map<String, Object> newFields = item.updatedMeasurementFields() != null
                    ? new LinkedHashMap<>(item.updatedMeasurementFields())
                    : (existingMeasurement != null ? new LinkedHashMap<>(existingMeasurement.getMeasurementFields()) : new LinkedHashMap<>());

            Map<String, Object> newDeltas = item.alterationDeltas() != null
                    ? new LinkedHashMap<>(item.alterationDeltas())
                    : new LinkedHashMap<>();

            if (existingMeasurement == null) {
                // Create dedicated measurement for this garment
                GarmentMeasurement newMeasurement = GarmentMeasurement.builder()
                        .user(customer)
                        .service(garment.getService())
                        .takenBy(currentUser != null ? currentUser : customer)
                        .measurementFields(newFields)
                        .alteration(newDeltas)
                        .note("Created during alteration for Order #" + order.getId())
                        .alterationNote(altNote)
                        .editable(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                GarmentMeasurement saved = garmentMeasurementRepository.save(newMeasurement);
                garment.setGarmentMeasurement(saved);
            } else {
                List<Garment> sharingGarments = garmentRepository.findByGarmentMeasurementId(existingMeasurement.getId());
                if (sharingGarments.size() > 1) {
                    // Shared measurement: fork a new measurement to isolate this garment
                    GarmentMeasurement forkedMeasurement = GarmentMeasurement.builder()
                        .user(customer)
                        .service(garment.getService())
                        .takenBy(currentUser != null ? currentUser : existingMeasurement.getTakenBy())
                        .measurementFields(newFields)
                        .alteration(newDeltas)
                        .note(existingMeasurement.getNote())
                        .alterationNote(altNote)
                        .editable(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                    GarmentMeasurement saved = garmentMeasurementRepository.save(forkedMeasurement);
                    garment.setGarmentMeasurement(saved);
                } else {
                    // Sole owner of this measurement: update directly
                    existingMeasurement.setMeasurementFields(newFields);
                    existingMeasurement.setAlteration(newDeltas);
                    existingMeasurement.setAlterationNote(altNote);
                    garmentMeasurementRepository.save(existingMeasurement);
                }
            }

            garmentRepository.save(garment);
            alteredGarmentIds.add(garment.getId());

            // Track latest alteration per service for updating customer defaults
            latestAlterationPerService.put(item.serviceId(), item);
        }

        // 3. Update customer default/latest measurement snapshots if requested
        boolean defaultsUpdated = false;
        if (request.updateCustomerDefaultMeasurements()) {
            for (Map.Entry<Long, GarmentAlterationItemDto> entry : latestAlterationPerService.entrySet()) {
                Long serviceId = entry.getKey();
                GarmentAlterationItemDto item = entry.getValue();

                TailoringService service = tailoringServiceRepository.findById(serviceId)
                        .orElse(null);

                if (service != null && item.updatedMeasurementFields() != null && !item.updatedMeasurementFields().isEmpty()) {
                    GarmentMeasurement defaultProfile = GarmentMeasurement.builder()
                            .user(customer)
                            .service(service)
                            .takenBy(currentUser != null ? currentUser : customer)
                            .measurementFields(new LinkedHashMap<>(item.updatedMeasurementFields()))
                            .alteration(item.alterationDeltas() != null ? new LinkedHashMap<>(item.alterationDeltas()) : new LinkedHashMap<>())
                            .note("Altered fit updated from Order #" + order.getId())
                            .alterationNote(item.alterationNote() != null ? item.alterationNote() : request.overallNote())
                            .editable(true)
                            .createdAt(LocalDateTime.now())
                            .build();

                    garmentMeasurementRepository.save(defaultProfile);
                    defaultsUpdated = true;
                }
            }
        }

        return new AlterationBatchResponseDto(
                order.getId(),
                customer.getId(),
                alteredGarmentIds.size(),
                defaultsUpdated,
                alteredGarmentIds,
                "Successfully marked " + alteredGarmentIds.size() + " garment(s) for alteration."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GarmentResponse> getActiveAlterations(Pageable pageable) {
        return garmentService.getGarmentsByStatus(GarmentStatus.ALTERATION, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlterationQueueItemDto> getAlterationQueue(
            GarmentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Garment> garments = garmentRepository.findAlterationQueue(status, startDate, endDate, trimmedSearch, pageable);

        return garments.map(g -> {
            Order o = g.getOrder();
            User c = o != null ? o.getCustomer() : null;
            GarmentMeasurement gm = g.getGarmentMeasurement();

            return new AlterationQueueItemDto(
                    g.getId(),
                    o != null ? o.getId() : null,
                    o != null ? ("ORD-" + o.getId()) : null,
                    o != null ? o.getOrderDate() : null,
                    o != null ? o.getSampleBookPageNumber() : null,

                    c != null ? c.getId() : null,
                    c != null ? c.getName() : "Unknown Customer",
                    c != null ? c.getPhone() : null,
                    c != null ? c.getCustomerCode() : null,

                    g.getService() != null ? g.getService().getId() : null,
                    g.getService() != null ? g.getService().getName() : "Unknown Service",
                    g.getSampleNumber(),
                    g.getStatus(),

                    g.getAlterationDeliveryDate(),
                    g.getAlterationNote(),
                    gm != null ? gm.getAlteration() : null,
                    gm != null ? gm.getMeasurementFields() : null,

                    g.getPrice(),
                    gm != null ? gm.getId() : null,

                    g.getCreatedAt(),
                    g.getUpdatedAt()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AlterationQueueSummaryDto getAlterationQueueSummary() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        long totalActive = garmentRepository.countByStatus(GarmentStatus.ALTERATION);
        long overdue = garmentRepository.countOverdueAlterations(GarmentStatus.ALTERATION, today);
        long dueToday = garmentRepository.countAlterationsOnDate(GarmentStatus.ALTERATION, today);
        long dueTomorrow = garmentRepository.countAlterationsOnDate(GarmentStatus.ALTERATION, tomorrow);
        long upcoming = garmentRepository.countAlterationsAfterDate(GarmentStatus.ALTERATION, tomorrow);
        long delivered = garmentRepository.countByStatus(GarmentStatus.DELIVERED);

        return new AlterationQueueSummaryDto(
                totalActive,
                overdue,
                dueToday,
                dueTomorrow,
                upcoming,
                delivered
        );
    }

    @Override
    public GarmentResponse quickDeliverAlteration(Long garmentId, String note) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + garmentId));

        garment.setStatus(GarmentStatus.DELIVERED);

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            garment.setDeliveredBy(currentUser);
        }

        if (note != null && !note.trim().isEmpty()) {
            String existing = garment.getNote();
            garment.setNote(existing != null && !existing.isBlank()
                    ? existing + " | Alteration Delivery: " + note.trim()
                    : "Alteration Delivery: " + note.trim());
        }

        garmentRepository.save(garment);
        return garmentService.getGarmentById(garment.getId());
    }

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return userRepository.findByEmail(auth.getName()).orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
