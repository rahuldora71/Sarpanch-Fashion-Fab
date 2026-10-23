//package com.sff.authapp.login.services.impl;
//
//import com.sff.authapp.login.dtos.measurement.MeasurementRequestDto;
//import com.sff.authapp.login.dtos.measurement.MeasurementResponse;
//import com.sff.authapp.login.dtos.garmentmeasurement.MeasurementSummaryResponse;
//import com.sff.authapp.login.dtos.measurement.MeasurementUpdateRequestDto;
//import com.sff.authapp.login.entities.Measurement;
//import com.sff.authapp.login.entities.MeasurementField;
//import com.sff.authapp.login.entities.TailoringService;
//import com.sff.authapp.login.entities.User;
//import com.sff.authapp.login.exceptions.ResourceNotFoundException;
//import com.sff.authapp.login.repositories.FieldRepository;
//import com.sff.authapp.login.repositories.MeasurementRepository;
//import com.sff.authapp.login.repositories.TailoringServiceRepository;
//import com.sff.authapp.login.repositories.UserRepository;
//import com.sff.authapp.login.services.MeasurementService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class MeasurementServiceImpl implements MeasurementService {
//
//    private final MeasurementRepository measurementRepository;
//
//    private final UserRepository userRepository;
//
//    private final TailoringServiceRepository tailoringServiceRepository;
//
//    private final FieldRepository fieldRepository;
//
//
//    // ============================================================
//    // CREATE
//    // ============================================================
//
//    @Override
//    public MeasurementResponse createMeasurement(
//            MeasurementRequestDto request
//    ) {
//
//        // --------------------------------------------------------
//        // Validate customer
//        // --------------------------------------------------------
//
//        User user = userRepository.findById(request.userId())
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Customer not found with id: "
//                                        + request.userId()
//                        )
//                );
//
//
//        // --------------------------------------------------------
//        // Validate service
//        // --------------------------------------------------------
//
//        TailoringService service =
//                tailoringServiceRepository.findById(
//                        request.serviceId()
//                ).orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Service not found with id: "
//                                        + request.serviceId()
//                        )
//                );
//
//
//        // --------------------------------------------------------
//        // Validate measurement field IDs
//        // --------------------------------------------------------
//
//        validateMeasurementFields(
//                service,
//                request.measurementFields()
//        );
//
//
//        // --------------------------------------------------------
//        // Validate required fields
//        // --------------------------------------------------------
//
//        validateRequiredMeasurementFields(
//                service,
//                request.measurementFields()
//        );
//
//
//        // --------------------------------------------------------
//        // Create measurement
//        // --------------------------------------------------------
//
//        Measurement measurement =
//                Measurement.builder()
//                        .user(user)
//                        .service(service)
//                        .measurementFields(
//                                request.measurementFields()
//                        )
//                        .takenBy(user)
//                        .note(request.note())
//                        .alterationNote(
//                                request.alterationNote()
//                        )
//                        .editable(
//                                request.editable() == null
//                                        || request.editable()
//                        )
//                        .build();
//
//
//        return toResponse(
//                measurementRepository.save(measurement)
//        );
//    }
//
//
//    // ============================================================
//    // GET BY ID
//    // ============================================================
//
//    @Override
//    @Transactional
//    public MeasurementResponse getMeasurementById(
//            Long measurementId
//    ) {
//
//        Measurement measurement =
//                measurementRepository.findById(measurementId)
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "Measurement not found with id: "
//                                                + measurementId
//                                )
//                        );
//
//        return toResponse(measurement);
//    }
//
//
//    // ============================================================
//    // CUSTOMER MEASUREMENTS
//    // ============================================================
//
//    @Override
//    @Transactional
//    public Page<MeasurementResponse> getCustomerMeasurements(
//            Long userId,
//            Pageable pageable
//    ) {
//
//        validateUser(userId);
//
//        return measurementRepository
//                .findByUserId(
//                        userId,
//                        pageable
//                )
//                .map(this::toResponse);
//    }
//
//
//    // ============================================================
//    // CUSTOMER + SERVICE MEASUREMENTS
//    // ============================================================
//
//    @Override
//    @Transactional
//    public Page<MeasurementResponse>
//    getCustomerServiceMeasurements(
//            Long userId,
//            Long serviceId,
//            Pageable pageable
//    ) {
//
//        validateUser(userId);
//
//        validateService(serviceId);
//
//        return measurementRepository
//                .findByUserIdAndServiceId(
//                        userId,
//                        serviceId,
//                        pageable
//                )
//                .map(this::toResponse);
//    }
//
//
//    // ============================================================
//    // LATEST MEASUREMENT
//    // ============================================================
//
//    @Override
//    @Transactional
//    public MeasurementResponse getLatestMeasurement(
//            Long userId,
//            Long serviceId
//    ) {
//
//        validateUser(userId);
//
//        validateService(serviceId);
//
//        Measurement measurement =
//                measurementRepository
//                        .findTopByUserIdAndServiceIdOrderByCreatedAtDescIdDesc(
//                                userId,
//                                serviceId
//                        )
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "No measurement found for customer "
//                                                + userId
//                                                + " and service "
//                                                + serviceId
//                                )
//                        );
//
//        return toResponse(measurement);
//    }
//
//
//    // ============================================================
//    // LATEST MEASUREMENT FOR EVERY SERVICE
//    // ============================================================
//
//    @Override
//    @Transactional
//    public List<MeasurementSummaryResponse>
//    getLatestMeasurementsPerService(
//            Long userId
//    ) {
//
//        validateUser(userId);
//
//        return measurementRepository
//                .findLatestMeasurementsPerService(userId)
//                .stream()
//                .map(this::toSummaryResponse)
//                .toList();
//    }
//
//
//    // ============================================================
//    // COPY SOURCES
//    // ============================================================
//
//    @Override
//    @Transactional
//    public List<MeasurementSummaryResponse> getCopySources(
//            Long userId,
//            Long targetServiceId
//    ) {
//
//        validateUser(userId);
//
//        validateService(targetServiceId);
//
//
//        /*
//         * Get the latest measurement for every service
//         * belonging to this customer.
//         *
//         * Example:
//         *
//         * Shirt     -> latest Shirt measurement
//         * Coat      -> latest Coat measurement
//         * Jodhpuri  -> latest Jodhpuri measurement
//         * Pant      -> latest Pant measurement
//         *
//         * Then remove the target service.
//         */
//
//        return measurementRepository
//                .findLatestMeasurementsPerService(userId)
//                .stream()
//                .filter(measurement ->
//                        measurement.getService() != null
//                                && !measurement
//                                .getService()
//                                .getId()
//                                .equals(targetServiceId)
//                )
//                .map(this::toSummaryResponse)
//                .toList();
//    }
//
//
//    // ============================================================
//    // UPDATE
//    // ============================================================
//
//    @Override
//    public MeasurementResponse updateMeasurement(
//            Long measurementId,
//            MeasurementUpdateRequestDto request
//    ) {
//
//        Measurement measurement =
//                measurementRepository.findById(measurementId)
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "Measurement not found with id: "
//                                                + measurementId
//                                )
//                        );
//
//
//        // --------------------------------------------------------
//        // Check whether measurement can be edited
//        // --------------------------------------------------------
//
//        if (!measurement.isEditable()) {
//
//            throw new IllegalStateException(
//                    "This measurement is locked and cannot be edited"
//            );
//        }
//
//
//        // --------------------------------------------------------
//        // Get service
//        // --------------------------------------------------------
//
//        TailoringService service =
//                measurement.getService();
//
//        if (service == null) {
//
//            throw new IllegalStateException(
//                    "Measurement is not associated with a service"
//            );
//        }
//
//
//        // --------------------------------------------------------
//        // Validate field IDs
//        // --------------------------------------------------------
//
//        validateMeasurementFields(
//                service,
//                request.measurementFields()
//        );
//
//
//        // --------------------------------------------------------
//        // Validate required fields
//        // --------------------------------------------------------
//
//        validateRequiredMeasurementFields(
//                service,
//                request.measurementFields()
//        );
//
//
//        // --------------------------------------------------------
//        // Update measurement values
//        // --------------------------------------------------------
//
//        measurement.setMeasurementFields(
//                request.measurementFields()
//        );
//
//
//        // --------------------------------------------------------
//        // Update note
//        // --------------------------------------------------------
//
//        if (request.note() != null) {
//
//            measurement.setNote(
//                    request.note()
//            );
//        }
//
//
//        // --------------------------------------------------------
//        // Update alteration note
//        // --------------------------------------------------------
//
//        if (request.alterationNote() != null) {
//
//            measurement.setAlterationNote(
//                    request.alterationNote()
//            );
//        }
//
//
//        // --------------------------------------------------------
//        // Update editable status
//        // --------------------------------------------------------
//
//        if (request.editable() != null) {
//
//            measurement.setEditable(
//                    request.editable()
//            );
//        }
//
//
//        return toResponse(measurement);
//    }
//
//
//    // ============================================================
//    // UPDATE EDITABLE STATUS
//    // ============================================================
//
//    @Override
//    public MeasurementResponse updateEditableStatus(
//            Long measurementId,
//            boolean editable
//    ) {
//
//        Measurement measurement =
//                measurementRepository.findById(measurementId)
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "Measurement not found with id: "
//                                                + measurementId
//                                )
//                        );
//
//
//        measurement.setEditable(editable);
//
//        return toResponse(measurement);
//    }
//
//
//    // ============================================================
//    // DELETE
//    // ============================================================
//
//    @Override
//    public void deleteMeasurement(
//            Long measurementId
//    ) {
//
//        Measurement measurement =
//                measurementRepository.findById(measurementId)
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "Measurement not found with id: "
//                                                + measurementId
//                                )
//                        );
//
//        measurementRepository.delete(measurement);
//    }
//
//
//    // ============================================================
//    // VALIDATE MEASUREMENT FIELDS
//    // ============================================================
//
//    /**
//     * Validates that:
//     *
//     * 1. Measurement JSON keys are valid Field IDs.
//     *
//     * 2. Every Field ID exists in the measurement_fields table.
//     *
//     * 3. Every Field ID belongs to the selected service.
//     *
//     * Example:
//     *
//     * Shirt service:
//     *
//     * 1 -> Chest
//     * 2 -> Shoulder
//     * 3 -> Sleeve
//     * 4 -> Shirt Length
//     *
//     * Valid:
//     *
//     * {
//     *     "1": 38,
//     *     "2": 17,
//     *     "3": 24,
//     *     "4": 29
//     * }
//     *
//     * Invalid:
//     *
//     * {
//     *     "1": 38,
//     *     "99": 50
//     * }
//     *
//     * if Field 99 does not exist.
//     */
//    private void validateMeasurementFields(
//            TailoringService service,
//            Map<String, Object> values
//    ) {
//
//        // --------------------------------------------------------
//        // Basic validation
//        // --------------------------------------------------------
//
//        if (values == null || values.isEmpty()) {
//
//            throw new IllegalArgumentException(
//                    "Measurement fields cannot be empty"
//            );
//        }
//
//
//        // --------------------------------------------------------
//        // Service template validation
//        // --------------------------------------------------------
//
//        if (service.getMeasurementFields() == null
//                || service.getMeasurementFields().isEmpty()) {
//
//            throw new IllegalArgumentException(
//                    "No measurement fields configured for service: "
//                            + service.getName()
//            );
//        }
//
//
//        // --------------------------------------------------------
//        // Get allowed Field IDs from service template
//        // --------------------------------------------------------
//
//        Set<Long> allowedFieldIds =
//                service.getMeasurementFields()
//                        .stream()
//                        .map(MeasurementField::getFieldId)
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toSet());
//
//
//        // --------------------------------------------------------
//        // Validate submitted Field IDs
//        // --------------------------------------------------------
//
//        for (String key : values.keySet()) {
//
//            Long fieldId;
//
//            try {
//
//                fieldId = Long.valueOf(key);
//
//            } catch (NumberFormatException ex) {
//
//                throw new IllegalArgumentException(
//                        "Invalid measurement field ID: "
//                                + key
//                );
//            }
//
//
//            // ----------------------------------------------------
//            // Field must exist
//            // ----------------------------------------------------
//
//            if (!fieldRepository.existsById(fieldId)) {
//
//                throw new ResourceNotFoundException(
//                        "Measurement field not found with id: "
//                                + fieldId
//                );
//            }
//
//
//            // ----------------------------------------------------
//            // Field must belong to service
//            // ----------------------------------------------------
//
//            if (!allowedFieldIds.contains(fieldId)) {
//
//                throw new IllegalArgumentException(
//                        "Measurement field "
//                                + fieldId
//                                + " does not belong to service "
//                                + service.getName()
//                );
//            }
//        }
//    }
//
//
//    // ============================================================
//    // VALIDATE REQUIRED MEASUREMENT FIELDS
//    // ============================================================
//
//    /**
//     * Validates all fields marked as required in the service
//     * measurement template.
//     *
//     * Example:
//     *
//     * Service:
//     *
//     * Chest        required
//     * Shoulder     required
//     * Sleeve       required
//     * Shirt Length required
//     *
//     * Request:
//     *
//     * {
//     *     "1": 38,
//     *     "2": 17,
//     *     "3": 24
//     * }
//     *
//     * Result:
//     *
//     * REJECTED
//     *
//     * because field 4 is required.
//     */
//    private void validateRequiredMeasurementFields(
//            TailoringService service,
//            Map<String, Object> values
//    ) {
//
//        if (service.getMeasurementFields() == null
//                || service.getMeasurementFields().isEmpty()) {
//
//            return;
//        }
//
//
//        for (MeasurementField field :
//                service.getMeasurementFields()) {
//
//            // ----------------------------------------------------
//            // Ignore optional fields
//            // ----------------------------------------------------
//
//            if (!field.isRequired()) {
//                continue;
//            }
//
//
//            Long fieldId = field.getFieldId();
//
//            if (fieldId == null) {
//                continue;
//            }
//
//
//            String key = String.valueOf(fieldId);
//
//
//            // ----------------------------------------------------
//            // Required field must exist and contain a value
//            // ----------------------------------------------------
//
//            if (!values.containsKey(key)
//                    || values.get(key) == null
//                    || values.get(key).toString().isBlank()) {
//
//                String fieldName =
//                        getFieldName(fieldId);
//
//                throw new IllegalArgumentException(
//                        "Required measurement field is missing: "
//                                + fieldName
//                                + " (fieldId: "
//                                + fieldId
//                                + ")"
//                );
//            }
//        }
//    }
//
//
//    // ============================================================
//    // GET FIELD NAME
//    // ============================================================
//
//    /**
//     * Used only for producing a useful validation error.
//     *
//     * Example:
//     *
//     * "Required measurement field is missing:
//     *  Chest (fieldId: 1)"
//     */
//    private String getFieldName(Long fieldId) {
//
//        return fieldRepository.findById(fieldId)
//                .map(field -> field.getName())
//                .orElse(
//                        "Unknown field"
//                );
//    }
//
//
//    // ============================================================
//    // VALIDATE USER
//    // ============================================================
//
//    private void validateUser(Long userId) {
//
//        if (!userRepository.existsById(userId)) {
//
//            throw new ResourceNotFoundException(
//                    "Customer not found with id: "
//                            + userId
//            );
//        }
//    }
//
//
//    // ============================================================
//    // VALIDATE SERVICE
//    // ============================================================
//
//    private void validateService(Long serviceId) {
//
//        if (!tailoringServiceRepository.existsById(serviceId)) {
//
//            throw new ResourceNotFoundException(
//                    "Service not found with id: "
//                            + serviceId
//            );
//        }
//    }
//
//
//    // ============================================================
//    // RESPONSE MAPPER
//    // ============================================================
//
//    private MeasurementResponse toResponse(
//            Measurement measurement
//    ) {
//
//        User takenBy =
//                measurement.getTakenBy();
//
//
//        return new MeasurementResponse(
//
//                // ------------------------------------------------
//                // Measurement ID
//                // ------------------------------------------------
//
//                measurement.getId(),
//
//
//                // ------------------------------------------------
//                // Customer ID
//                // ------------------------------------------------
//
//                measurement.getUser() != null
//                        ? measurement.getUser().getId()
//                        : null,
//
//
//                // ------------------------------------------------
//                // Service ID
//                // ------------------------------------------------
//
//                measurement.getService() != null
//                        ? measurement.getService().getId()
//                        : null,
//
//
//                // ------------------------------------------------
//                // Service Name
//                // ------------------------------------------------
//
//                measurement.getService() != null
//                        ? measurement.getService().getName()
//                        : null,
//
//
//                // ------------------------------------------------
//                // Measurement values
//                // ------------------------------------------------
//
//                measurement.getMeasurementFields() != null
//                        ? measurement.getMeasurementFields()
//                        : Collections.emptyMap(),
//
//
//                // ------------------------------------------------
//                // Taken By
//                // ------------------------------------------------
//
//                takenBy != null
//                        ? takenBy.getId()
//                        : null,
//
//
//                // ------------------------------------------------
//                // Editable
//                // ------------------------------------------------
//
//                measurement.isEditable(),
//
//
//                // ------------------------------------------------
//                // Note
//                // ------------------------------------------------
//
//                measurement.getNote(),
//
//
//                // ------------------------------------------------
//                // Alteration note
//                // ------------------------------------------------
//
//                measurement.getAlterationNote(),
//
//
//                // ------------------------------------------------
//                // Created At
//                // ------------------------------------------------
//
//                measurement.getCreatedAt(),
//
//
//                // ------------------------------------------------
//                // Updated At
//                // ------------------------------------------------
//
//                measurement.getUpdatedAt()
//        );
//    }
//
//
//    // ============================================================
//    // SUMMARY RESPONSE MAPPER
//    // ============================================================
//
//    private MeasurementSummaryResponse toSummaryResponse(
//            Measurement measurement
//    ) {
//
//        return new MeasurementSummaryResponse(
//
//                // ------------------------------------------------
//                // Measurement ID
//                // ------------------------------------------------
//
//                measurement.getId(),
//
//
//                // ------------------------------------------------
//                // Service ID
//                // ------------------------------------------------
//
//                measurement.getService() != null
//                        ? measurement.getService().getId()
//                        : null,
//
//
//                // ------------------------------------------------
//                // Service Name
//                // ------------------------------------------------
//
//                measurement.getService() != null
//                        ? measurement.getService().getName()
//                        : null,
//
//
//                // ------------------------------------------------
//                // Measurement values
//                // ------------------------------------------------
//
//                measurement.getMeasurementFields() != null
//                        ? measurement.getMeasurementFields()
//                        : Collections.emptyMap(),
//
//
//                // ------------------------------------------------
//                // Measurement date
//                // ------------------------------------------------
//
//                measurement.getCreatedAt()
//        );
//    }
//}