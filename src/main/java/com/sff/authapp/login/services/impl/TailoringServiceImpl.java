package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.service.ServiceResponse;
import com.sff.authapp.login.dtos.service.TailoringServiceRequestDto;
import com.sff.authapp.login.dtos.service.TailoringServiceUpdateRequestDto;
import com.sff.authapp.login.entities.MeasurementField;
import com.sff.authapp.login.entities.TailoringService;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.FieldRepository;
import com.sff.authapp.login.repositories.TailoringServiceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TailoringServiceImpl implements com.sff.authapp.login.services.TailoringService {

    private final TailoringServiceRepository serviceRepository;

    private final FieldRepository fieldRepository;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public ServiceResponse createService(TailoringServiceRequestDto request) {

        String name = normalize(request.name());

        if (serviceRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "Service already exists with name: " + name
            );
        }

        validateMeasurementFields(request.measurementFields());

        TailoringService service = TailoringService.builder()
                .name(name)
                .category(normalize(request.category()))
                .price(request.price())
                .active(request.active())
                .tailorCommission(request.tailorCommission())
                .measurementFields(request.measurementFields())
                .build();

        TailoringService savedService = serviceRepository.save(service);

        return toResponse(savedService);
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long serviceId) {

        TailoringService service = findById(serviceId);

        return toResponse(service);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getAllServices(Pageable pageable) {

        return serviceRepository
                .findAll(pageable)
                .map(this::toResponse);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> searchServices(
            String keyword,
            Pageable pageable
    ) {

        String normalizedKeyword = normalize(keyword);

        return serviceRepository
                .findByNameContainingIgnoreCase(
                        normalizedKeyword,
                        pageable
                )
                .map(this::toResponse);
    }

    // =========================================================
    // ACTIVE SERVICES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getActiveServices(Pageable pageable) {

        return serviceRepository
                .findByActiveTrue(pageable)
                .map(this::toResponse);
    }

    // =========================================================
    // INACTIVE SERVICES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getInactiveServices(Pageable pageable) {

        return serviceRepository
                .findByActiveFalse(pageable)
                .map(this::toResponse);
    }

    // =========================================================
    // SERVICES BY CATEGORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getServicesByCategory(
            String category,
            Pageable pageable
    ) {

        return serviceRepository
                .findByCategoryIgnoreCase(
                        normalize(category),
                        pageable
                )
                .map(this::toResponse);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public ServiceResponse updateService(
            Long serviceId,
            TailoringServiceUpdateRequestDto request
    ) {

        TailoringService service = findById(serviceId);

        if (request.name() != null) {

            String name = normalize(request.name());

            if (!name.equalsIgnoreCase(service.getName())
                    && serviceRepository.existsByNameIgnoreCase(name)) {

                throw new IllegalArgumentException(
                        "Service already exists with name: " + name
                );
            }

            service.setName(name);
        }

        if (request.category() != null) {
            service.setCategory(normalize(request.category()));
        }

        if (request.price() != null) {
            service.setPrice(request.price());
        }

        if (request.active() != null) {
            service.setActive(request.active());
        }

        if (request.tailorCommission() != null) {
            service.setTailorCommission(request.tailorCommission());
        }

        if (request.measurementFields() != null) {

            validateMeasurementFields(
                    request.measurementFields()
            );

            service.setMeasurementFields(
                    request.measurementFields()
            );
        }

        TailoringService updatedService =
                serviceRepository.save(service);

        return toResponse(updatedService);
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @Override
    public ServiceResponse updateServiceStatus(
            Long serviceId,
            boolean active
    ) {

        TailoringService service = findById(serviceId);

        service.setActive(active);

        TailoringService updatedService =
                serviceRepository.save(service);

        return toResponse(updatedService);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteService(Long serviceId) {

        TailoringService service = findById(serviceId);

        /*
         * Soft delete.
         *
         * We don't actually remove the service from the database.
         * Instead, we mark it inactive.
         */
        service.setActive(false);

        serviceRepository.save(service);
    }

    // =========================================================
    // FIND SERVICE
    // =========================================================

    private TailoringService findById(Long serviceId) {

        return serviceRepository
                .findById(serviceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Service not found with id: " + serviceId
                        )
                );
    }

    // =========================================================
    // VALIDATE MEASUREMENT FIELDS
    // =========================================================

    private void validateMeasurementFields(
            List<MeasurementField> measurementFields
    ) {

        if (measurementFields == null
                || measurementFields.isEmpty()) {
            return;
        }

        for (MeasurementField measurementField : measurementFields) {

            if (measurementField == null) {
                throw new IllegalArgumentException(
                        "Measurement field cannot be null"
                );
            }

            if (measurementField.getFieldId() == null) {
                throw new IllegalArgumentException(
                        "Measurement field ID is required"
                );
            }

            boolean exists =
                    fieldRepository.existsById(
                            measurementField.getFieldId()
                    );

            if (!exists) {
                throw new ResourceNotFoundException(
                        "Measurement field not found with id: "
                                + measurementField.getFieldId()
                );
            }
        }
    }

    // =========================================================
    // MAPPER
    // =========================================================

    private ServiceResponse toResponse(
            TailoringService service
    ) {

        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getCategory(),
                service.getPrice(),
                service.isActive(),
                service.getTailorCommission(),
                service.getMeasurementFields(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }

    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}