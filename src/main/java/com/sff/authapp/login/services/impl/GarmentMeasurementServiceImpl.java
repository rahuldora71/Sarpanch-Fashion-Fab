package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.garmentmeasurement.CustomerMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementSummaryResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementUpdateRequestDto;
import com.sff.authapp.login.entities.Field;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentMeasurement;
import com.sff.authapp.login.entities.MeasurementField;
import com.sff.authapp.login.entities.TailoringService;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.FieldRepository;
import com.sff.authapp.login.repositories.GarmentMeasurementRepository;
import com.sff.authapp.login.repositories.GarmentRepository;
import com.sff.authapp.login.repositories.TailoringServiceRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.services.GarmentMeasurementService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GarmentMeasurementServiceImpl
        implements GarmentMeasurementService {

    private final GarmentMeasurementRepository garmentMeasurementRepository;

    private final GarmentRepository garmentRepository;

    private final FieldRepository fieldRepository;

    private final UserRepository userRepository;

    private final TailoringServiceRepository tailoringServiceRepository;


    // ============================================================
    // CREATE
    // ============================================================

    /**
     * Creates a new measurement for a garment.
     *
     * <p>
     * Customer and service are derived from the garment and its order.
     * The created measurement is then assigned to the garment.
     * </p>
     *
     * <p>
     * A measurement is now reusable. Multiple garments can reference
     * the same GarmentMeasurement.
     * </p>
     */
    @Override
    public GarmentMeasurementResponse createMeasurement(
            GarmentMeasurementRequestDto request
    ) {

        Garment garment = findGarment(request.garmentId());

        if (garment.getOrder() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Garment is not associated with an order"
            );
        }

        if (garment.getOrder().getCustomer() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Garment order is not associated with a customer"
            );
        }

        if (garment.getService() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Garment is not associated with a tailoring service"
            );
        }

        /*
         * A garment can have only one measurement reference.
         *
         * This does NOT mean that a measurement can belong to only one
         * garment. The same measurement can now be reused by multiple
         * garments.
         */
        if (garment.getGarmentMeasurement() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Measurement already exists for this garment"
            );
        }

        validateFields(
                garment,
                request.measurementFields()
        );

        validateRequiredFields(
                garment,
                request.measurementFields()
        );

        GarmentMeasurement measurement =
                GarmentMeasurement.builder()
                        .service(garment.getService())
                        .user(garment.getOrder().getCustomer())
                        .measurementFields(
                                new LinkedHashMap<>(
                                        request.measurementFields()
                                )
                        )
                        .alteration(
                                request.alteration() == null
                                        ? new LinkedHashMap<>()
                                        : new LinkedHashMap<>(
                                        request.alteration()
                                )
                        )
                        .note(request.note())
                        .alterationNote(
                                request.alterationNote()
                        )
                        .editable(
                                request.editable() == null
                                        || request.editable()
                        )
                        .takenBy(
                                request.takenByUserId() == null
                                        ? null
                                        : userRepository
                                        .findById(
                                                request.takenByUserId()
                                        )
                                        .orElseThrow(() ->
                                                     new ResponseStatusException(
                                                             HttpStatus.NOT_FOUND,
                                                             "User not found with id: "
                                                             + request.takenByUserId()
                                                     )
                                        )
                        )
                        .build();

        GarmentMeasurement saved =
                garmentMeasurementRepository.save(
                        measurement
                );

        /*
         * New relationship:
         *
         * Garment -> GarmentMeasurement
         */
        garment.setGarmentMeasurement(saved);

        garmentRepository.save(garment);

        return mapToResponse(saved);
    }


    // ============================================================
    // CREATE CUSTOMER MEASUREMENT (DIRECT / STANDALONE)
    // ============================================================

    @Override
    public GarmentMeasurementResponse createCustomerMeasurement(
            CustomerMeasurementRequestDto request
    ) {
        User customer = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with id: " + request.userId()
                ));

        TailoringService service = tailoringServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Service not found with id: " + request.serviceId()
                ));

        if (!service.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tailoring service is inactive: " + service.getName()
            );
        }

        validateFields(service, request.measurementFields());
        validateRequiredFields(service, request.measurementFields());

        User takenBy = null;
        if (request.takenByUserId() != null) {
            takenBy = userRepository.findById(request.takenByUserId()).orElse(null);
        }
        if (takenBy == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User userPrincipal) {
                takenBy = userPrincipal;
            }
        }
        if (takenBy == null) {
            takenBy = customer;
        }

        GarmentMeasurement measurement = GarmentMeasurement.builder()
                .service(service)
                .user(customer)
                .measurementFields(
                        request.measurementFields() == null
                                ? new LinkedHashMap<>()
                                : new LinkedHashMap<>(request.measurementFields())
                )
                .alteration(
                        request.alteration() == null
                                ? new LinkedHashMap<>()
                                : new LinkedHashMap<>(request.alteration())
                )
                .note(request.note())
                .alterationNote(request.alterationNote())
                .editable(request.editable() == null || request.editable())
                .takenBy(takenBy)
                .build();

        GarmentMeasurement saved = garmentMeasurementRepository.save(measurement);

        return mapToResponse(saved);
    }


    // ============================================================
    // GET ALL MEASUREMENTS (PAGINATED WITH SEARCH)
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<GarmentMeasurementResponse> getAllMeasurements(
            String keyword,
            Pageable pageable
    ) {
        String trimmed = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return garmentMeasurementRepository.findAllWithSearch(trimmed, pageable)
                .map(this::mapToResponse);
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GarmentMeasurementResponse getMeasurementById(
            Long measurementId
    ) {

        return mapToResponse(
                findMeasurement(measurementId)
        );
    }


    // ============================================================
    // GET BY GARMENT
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GarmentMeasurementResponse getMeasurementByGarment(
            Long garmentId
    ) {

        Garment garment = findGarment(garmentId);

        GarmentMeasurement measurement =
                garment.getGarmentMeasurement();

        if (measurement == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No measurement found for garment"
            );
        }

        return mapToResponseForGarment(
                measurement,
                garment
        );
    }


    // ============================================================
    // CUSTOMER HISTORY
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<GarmentMeasurementResponse>
    getCustomerMeasurements(
            Long userId,
            Pageable pageable
    ) {

        validateUser(userId);

        return garmentMeasurementRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(
                        userId,
                        pageable
                )
                .map(this::mapToResponse);
    }


    // ============================================================
    // CUSTOMER + SERVICE HISTORY
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<GarmentMeasurementResponse>
    getCustomerServiceMeasurements(
            Long userId,
            Long serviceId,
            Pageable pageable
    ) {

        validateUser(userId);
        validateService(serviceId);

        return garmentMeasurementRepository
                .findByUserIdAndServiceIdOrderByCreatedAtDescIdDesc(
                        userId,
                        serviceId,
                        pageable
                )
                .map(this::mapToResponse);
    }


    // ============================================================
    // LATEST CUSTOMER + SERVICE
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GarmentMeasurementResponse getLatestMeasurement(
            Long userId,
            Long serviceId
    ) {

        validateUser(userId);
        validateService(serviceId);

        GarmentMeasurement measurement =
                garmentMeasurementRepository
                        .findTopByUserIdAndServiceIdOrderByCreatedAtDescIdDesc(
                                userId,
                                serviceId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No measurement found for customer "
                                                + userId
                                                + " and service "
                                                + serviceId
                                )
                        );

        return mapToResponse(measurement);
    }


    // ============================================================
    // LATEST MEASUREMENT PER SERVICE
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<GarmentMeasurementSummaryResponse>
    getLatestMeasurementsPerService(
            Long userId
    ) {

        validateUser(userId);

        return garmentMeasurementRepository
                .findLatestMeasurementsPerService(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }


    // ============================================================
    // COPY SOURCES
    // ============================================================

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<GarmentMeasurementSummaryResponse>
    getCopySources(
            Long userId,
            Long targetServiceId
    ) {

        validateUser(userId);
        validateService(targetServiceId);

        return garmentMeasurementRepository
                .findLatestMeasurementsPerService(userId)
                .stream()
                .filter(measurement ->
                        measurement.getService() != null
                                && !Objects.equals(
                                measurement.getService().getId(),
                                targetServiceId
                        )
                )
                .map(this::toSummaryResponse)
                .toList();
    }


    // ============================================================
    // UPDATE
    // ============================================================

    /**
     * Updates an existing measurement.
     *
     * <p>
     * Because a measurement can now be reused by multiple garments,
     * updating it changes the shared measurement snapshot.
     *
     * For historical immutability, create a new measurement instead
     * of modifying a measurement that is already being used by
     * multiple garments.
     * </p>
     */
    @Override
    public GarmentMeasurementResponse updateMeasurement(
            Long measurementId,
            GarmentMeasurementUpdateRequestDto request
    ) {

        GarmentMeasurement measurement =
                findMeasurement(measurementId);

        if (!measurement.isEditable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Garment measurement is locked"
            );
        }

        /*
         * The measurement no longer owns/contains a Garment reference.
         *
         * Validation must therefore use the service stored directly
         * on GarmentMeasurement.
         */
        validateMeasurementFields(
                measurement,
                request.measurementFields()
        );

        validateRequiredMeasurementFields(
                measurement,
                request.measurementFields()
        );

        measurement.setMeasurementFields(
                new LinkedHashMap<>(
                        request.measurementFields()
                )
        );

        if (request.alteration() != null) {
            measurement.setAlteration(
                    new LinkedHashMap<>(
                            request.alteration()
                    )
            );
        }

        if (request.note() != null) {
            measurement.setNote(
                    request.note()
            );
        }

        if (request.alterationNote() != null) {
            measurement.setAlterationNote(
                    request.alterationNote()
            );
        }

        if (request.editable() != null) {
            measurement.setEditable(
                    request.editable()
            );
        }

        return mapToResponse(
                garmentMeasurementRepository.save(
                        measurement
                )
        );
    }


    // ============================================================
    // LOCK / UNLOCK
    // ============================================================

    @Override
    public GarmentMeasurementResponse updateEditableStatus(
            Long measurementId,
            boolean editable
    ) {

        GarmentMeasurement measurement =
                findMeasurement(measurementId);

        measurement.setEditable(editable);

        return mapToResponse(
                garmentMeasurementRepository.save(
                        measurement
                )
        );
    }


    // ============================================================
    // DELETE
    // ============================================================

    /**
     * Deletes a measurement only when it is not referenced by any
     * garment.
     */
    @Override
    public void deleteMeasurement(
            Long measurementId
    ) {

        GarmentMeasurement measurement =
                findMeasurement(measurementId);

        /*
         * Since Garment now owns the relationship, deleting the
         * measurement directly can violate the foreign key when
         * garments still reference it.
         *
         * Therefore, first find all garments referencing it.
         *
         * This requires GarmentRepository to provide a suitable
         * query if you want full reference cleanup.
         */
        List<Garment> garments =
                garmentRepository
                        .findByGarmentMeasurementId(
                                measurementId
                        );

        if (!garments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete measurement because it is being used by "
                            + garments.size()
                            + " garment(s)"
            );
        }

        garmentMeasurementRepository.delete(
                measurement
        );
    }


    // ============================================================
    // FIND GARMENT
    // ============================================================

    private Garment findGarment(
            Long garmentId
    ) {

        return garmentRepository
                .findById(garmentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment not found with id: "
                                        + garmentId
                        )
                );
    }


    // ============================================================
    // FIND MEASUREMENT
    // ============================================================

    private GarmentMeasurement findMeasurement(
            Long measurementId
    ) {

        return garmentMeasurementRepository
                .findById(measurementId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment measurement not found with id: "
                                        + measurementId
                        )
                );
    }


    // ============================================================
    // VALIDATE FIELDS USING SERVICE
    // ============================================================

    private void validateFields(
            TailoringService service,
            Map<String, Object> values
    ) {

        if (values == null || values.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Measurement fields are required"
            );
        }

        if (service == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tailoring service is required"
            );
        }

        if (service.getMeasurementFields() == null
                || service.getMeasurementFields().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No measurement fields configured for service: "
                            + service.getName()
            );
        }

        Set<Long> serviceFieldIds =
                service.getMeasurementFields()
                        .stream()
                        .map(MeasurementField::getFieldId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        for (String key : values.keySet()) {

            Long fieldId;

            try {

                fieldId = Long.parseLong(key);

            } catch (NumberFormatException exception) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid measurement field ID: "
                                + key
                );
            }

            if (!fieldRepository.existsById(fieldId)) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Measurement field not found: "
                                + fieldId
                );
            }

            if (!serviceFieldIds.contains(fieldId)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Measurement field "
                                + fieldId
                                + " does not belong to service "
                                + service.getName()
                );
            }
        }
    }


    // ============================================================
    // VALIDATE FIELDS USING GARMENT
    // ============================================================

    private void validateFields(
            Garment garment,
            Map<String, Object> values
    ) {

        if (garment.getService() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Garment is not associated with a service"
            );
        }

        validateFields(garment.getService(), values);
    }


    // ============================================================
    // VALIDATE REQUIRED FIELDS USING SERVICE
    // ============================================================

    private void validateRequiredFields(
            TailoringService service,
            Map<String, Object> values
    ) {

        if (service == null || service.getMeasurementFields() == null) {

            return;
        }

        for (MeasurementField field :
                service.getMeasurementFields()) {

            if (!field.isRequired()) {
                continue;
            }

            Long fieldId =
                    field.getFieldId();

            if (fieldId == null) {
                continue;
            }

            String key =
                    String.valueOf(fieldId);

            Object value =
                    values.get(key);

            if (value == null
                    || value.toString().isBlank()) {

                Field fieldEntity =
                        fieldRepository
                                .findById(fieldId)
                                .orElseThrow(() ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Measurement field not found: "
                                                        + fieldId
                                        )
                                );

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Required measurement field is missing: "
                                + fieldEntity.getName()
                                + " (fieldId: "
                                + fieldId
                                + ")"
                );
            }
        }
    }


    // ============================================================
    // VALIDATE REQUIRED FIELDS USING GARMENT
    // ============================================================

    private void validateRequiredFields(
            Garment garment,
            Map<String, Object> values
    ) {

        if (garment.getService() != null) {
            validateRequiredFields(garment.getService(), values);
        }
    }


    // ============================================================
    // VALIDATE FIELDS USING MEASUREMENT
    // ============================================================

    private void validateMeasurementFields(
            GarmentMeasurement measurement,
            Map<String, Object> values
    ) {

        if (values == null || values.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Measurement fields are required"
            );
        }

        if (measurement.getService() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Measurement is not associated with a tailoring service"
            );
        }

        if (measurement.getService().getMeasurementFields() == null
                || measurement.getService()
                .getMeasurementFields()
                .isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No measurement fields configured for service: "
                            + measurement.getService().getName()
            );
        }

        Set<Long> serviceFieldIds =
                measurement.getService()
                        .getMeasurementFields()
                        .stream()
                        .map(MeasurementField::getFieldId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        for (String key : values.keySet()) {

            Long fieldId;

            try {

                fieldId = Long.parseLong(key);

            } catch (NumberFormatException exception) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid measurement field ID: "
                                + key
                );
            }

            if (!fieldRepository.existsById(fieldId)) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Measurement field not found: "
                                + fieldId
                );
            }

            if (!serviceFieldIds.contains(fieldId)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Measurement field "
                                + fieldId
                                + " does not belong to service "
                                + measurement.getService().getName()
                );
            }
        }
    }


    // ============================================================
    // VALIDATE REQUIRED FIELDS USING MEASUREMENT
    // ============================================================

    private void validateRequiredMeasurementFields(
            GarmentMeasurement measurement,
            Map<String, Object> values
    ) {

        if (measurement.getService() == null
                || measurement.getService().getMeasurementFields() == null) {

            return;
        }

        for (MeasurementField field :
                measurement.getService().getMeasurementFields()) {

            if (!field.isRequired()) {
                continue;
            }

            Long fieldId =
                    field.getFieldId();

            if (fieldId == null) {
                continue;
            }

            String key =
                    String.valueOf(fieldId);

            Object value =
                    values.get(key);

            if (value == null
                    || value.toString().isBlank()) {

                Field fieldEntity =
                        fieldRepository
                                .findById(fieldId)
                                .orElseThrow(() ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Measurement field not found: "
                                                        + fieldId
                                        )
                                );

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Required measurement field is missing: "
                                + fieldEntity.getName()
                                + " (fieldId: "
                                + fieldId
                                + ")"
                );
            }
        }
    }


    // ============================================================
    // VALIDATE USER
    // ============================================================

    private void validateUser(
            Long userId
    ) {

        if (!userRepository.existsById(userId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Customer not found with id: "
                            + userId
            );
        }
    }


    // ============================================================
    // VALIDATE SERVICE
    // ============================================================

    private void validateService(
            Long serviceId
    ) {

        if (!tailoringServiceRepository
                .existsById(serviceId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Service not found with id: "
                            + serviceId
            );
        }
    }


    // ============================================================
    // MAP FULL RESPONSE
    // ============================================================

    /**
     * Maps a measurement to the API response.
     *
     * <p>
     * GarmentMeasurement no longer contains a Garment reference.
     * Therefore, the garment information is obtained from the
     * GarmentRepository.
     * </p>
     *
     * <p>
     * If the measurement has been reused by multiple garments,
     * the first garment is used for the legacy garmentId/orderId
     * fields in this response.
     * </p>
     */
    private GarmentMeasurementResponse mapToResponse(
            GarmentMeasurement measurement
    ) {

        Garment garment =
                garmentRepository
                        .findFirstByGarmentMeasurementIdOrderByIdAsc(
                                measurement.getId()
                        )
                        .orElse(null);

        Long garmentId =
                garment != null
                        ? garment.getId()
                        : null;

        Long orderId =
                garment != null
                        && garment.getOrder() != null
                        ? garment.getOrder().getId()
                        : null;

        Long customerId =
                measurement.getUser() != null
                        ? measurement.getUser().getId()
                        : null;

        String customerName =
                measurement.getUser() != null
                        ? measurement.getUser().getName()
                        : null;

        String customerPhone =
                measurement.getUser() != null
                        ? measurement.getUser().getPhone()
                        : null;

        Long serviceId =
                measurement.getService() != null
                        ? measurement.getService().getId()
                        : null;

        String serviceName =
                measurement.getService() != null
                        ? measurement.getService().getName()
                        : null;

        Long takenByUserId =
                measurement.getTakenBy() != null
                        ? measurement.getTakenBy().getId()
                        : null;

        return new GarmentMeasurementResponse(

                measurement.getId(),

                garmentId,

                orderId,

                customerId,

                customerName,

                customerPhone,

                serviceId,

                serviceName,

                measurement.getMeasurementFields() != null
                        ? measurement.getMeasurementFields()
                        : Collections.emptyMap(),

                measurement.getAlteration() != null
                        ? measurement.getAlteration()
                        : Collections.emptyMap(),

                takenByUserId,

                measurement.isEditable(),

                measurement.getNote(),

                measurement.getAlterationNote(),

                measurement.getCreatedAt(),

                measurement.getUpdatedAt()
        );
    }


    // ============================================================
    // MAP RESPONSE FOR SPECIFIC GARMENT
    // ============================================================

    private GarmentMeasurementResponse mapToResponseForGarment(
            GarmentMeasurement measurement,
            Garment garment
    ) {

        Long orderId =
                garment.getOrder() != null
                        ? garment.getOrder().getId()
                        : null;

        Long customerId =
                measurement.getUser() != null
                        ? measurement.getUser().getId()
                        : null;

        String customerName =
                measurement.getUser() != null
                        ? measurement.getUser().getName()
                        : null;

        String customerPhone =
                measurement.getUser() != null
                        ? measurement.getUser().getPhone()
                        : null;

        Long serviceId =
                measurement.getService() != null
                        ? measurement.getService().getId()
                        : null;

        String serviceName =
                measurement.getService() != null
                        ? measurement.getService().getName()
                        : null;

        Long takenByUserId =
                measurement.getTakenBy() != null
                        ? measurement.getTakenBy().getId()
                        : null;

        return new GarmentMeasurementResponse(

                measurement.getId(),

                garment.getId(),

                orderId,

                customerId,

                customerName,

                customerPhone,

                serviceId,

                serviceName,

                measurement.getMeasurementFields() != null
                        ? measurement.getMeasurementFields()
                        : Collections.emptyMap(),

                measurement.getAlteration() != null
                        ? measurement.getAlteration()
                        : Collections.emptyMap(),

                takenByUserId,

                measurement.isEditable(),

                measurement.getNote(),

                measurement.getAlterationNote(),

                measurement.getCreatedAt(),

                measurement.getUpdatedAt()
        );
    }


    // ============================================================
    // MAP SUMMARY RESPONSE
    // ============================================================

    private GarmentMeasurementSummaryResponse
    toSummaryResponse(
            GarmentMeasurement measurement
    ) {

        Garment garment =
                garmentRepository
                        .findFirstByGarmentMeasurementIdOrderByIdAsc(
                                measurement.getId()
                        )
                        .orElse(null);

        Long garmentId =
                garment != null
                        ? garment.getId()
                        : null;

        Long orderId =
                garment != null
                        && garment.getOrder() != null
                        ? garment.getOrder().getId()
                        : null;

        Long serviceId =
                measurement.getService() != null
                        ? measurement.getService().getId()
                        : null;

        String serviceName =
                measurement.getService() != null
                        ? measurement.getService().getName()
                        : null;

        return new GarmentMeasurementSummaryResponse(

                measurement.getId(),

                garmentId,

                orderId,

                serviceId,

                serviceName,

                measurement.getMeasurementFields() != null
                        ? measurement.getMeasurementFields()
                        : Collections.emptyMap(),

                measurement.getCreatedAt()
        );
    }
}