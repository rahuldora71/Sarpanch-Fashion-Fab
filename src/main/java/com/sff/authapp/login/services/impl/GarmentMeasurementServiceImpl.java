package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementUpdateRequestDto;
import com.sff.authapp.login.entities.Field;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentMeasurement;
import com.sff.authapp.login.entities.MeasurementField;
import com.sff.authapp.login.repositories.FieldRepository;
import com.sff.authapp.login.repositories.GarmentMeasurementRepository;
import com.sff.authapp.login.repositories.GarmentRepository;
import com.sff.authapp.login.services.GarmentMeasurementService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GarmentMeasurementServiceImpl
        implements GarmentMeasurementService {

    private final GarmentMeasurementRepository
            garmentMeasurementRepository;

    private final GarmentRepository garmentRepository;

    private final FieldRepository fieldRepository;


    @Override
    public GarmentMeasurementResponse createMeasurement(
            GarmentMeasurementRequestDto request
    ) {

        Garment garment = findGarment(
                request.garmentId()
        );

        if (garmentMeasurementRepository
                .existsByGarmentId(garment.getId())) {

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
                        .garment(garment)
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
                        .build();

        GarmentMeasurement saved =
                garmentMeasurementRepository.save(measurement);

        garment.setGarmentMeasurement(saved);

        return mapToResponse(saved);
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GarmentMeasurementResponse getMeasurementById(
            Long measurementId
    ) {

        return mapToResponse(
                findMeasurement(measurementId)
        );
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GarmentMeasurementResponse getMeasurementByGarment(
            Long garmentId
    ) {

        GarmentMeasurement measurement =
                garmentMeasurementRepository
                        .findByGarmentId(garmentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Garment measurement not found"
                                )
                        );

        return mapToResponse(measurement);
    }


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

        validateFields(
                measurement.getGarment(),
                request.measurementFields()
        );

        validateRequiredFields(
                measurement.getGarment(),
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
            measurement.setNote(request.note());
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
                garmentMeasurementRepository.save(measurement)
        );
    }


    @Override
    public GarmentMeasurementResponse updateEditableStatus(
            Long measurementId,
            boolean editable
    ) {

        GarmentMeasurement measurement =
                findMeasurement(measurementId);

        measurement.setEditable(editable);

        return mapToResponse(
                garmentMeasurementRepository.save(measurement)
        );
    }


    @Override
    public void deleteMeasurement(
            Long measurementId
    ) {

        GarmentMeasurement measurement =
                findMeasurement(measurementId);

        garmentMeasurementRepository.delete(measurement);
    }


    private Garment findGarment(
            Long garmentId
    ) {

        return garmentRepository.findById(garmentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment not found"
                        )
                );
    }


    private GarmentMeasurement findMeasurement(
            Long measurementId
    ) {

        return garmentMeasurementRepository.findById(
                measurementId
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Garment measurement not found"
                )
        );
    }


    private void validateFields(
            Garment garment,
            Map<String, Object> values
    ) {

        if (values == null || values.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Measurement fields are required"
            );
        }

        Set<Long> serviceFieldIds =
                garment.getService()
                        .getMeasurementFields()
                        .stream()
                        .map(MeasurementField::getFieldId)
                        .collect(Collectors.toSet());

        for (String key : values.keySet()) {

            Long fieldId;

            try {
                fieldId = Long.parseLong(key);
            } catch (NumberFormatException exception) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid measurement field ID: " + key
                );
            }

            if (!fieldRepository.existsById(fieldId)) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Measurement field not found: " + fieldId
                );
            }

            if (!serviceFieldIds.contains(fieldId)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Measurement field "
                                + fieldId
                                + " does not belong to service "
                                + garment.getService().getName()
                );
            }
        }
    }


    private void validateRequiredFields(
            Garment garment,
            Map<String, Object> values
    ) {

        for (MeasurementField field :
                garment.getService().getMeasurementFields()) {

            if (!field.isRequired()) {
                continue;
            }

            String key =
                    String.valueOf(field.getFieldId());

            Object value = values.get(key);

            if (value == null) {

                Field fieldEntity =
                        fieldRepository.findById(
                                field.getFieldId()
                        ).orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Measurement field not found"
                                )
                        );

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Required measurement field is missing: "
                                + fieldEntity.getName()
                );
            }
        }
    }


    private GarmentMeasurementResponse mapToResponse(
            GarmentMeasurement measurement
    ) {

        Garment garment = measurement.getGarment();

        return new GarmentMeasurementResponse(
                measurement.getId(),
                garment.getId(),
                garment.getOrder().getId(),
                garment.getOrder().getCustomer().getId(),
                measurement.getService().getId(),
                measurement.getService().getName(),
                measurement.getMeasurementFields(),
                measurement.getAlteration(),
                measurement.getTakenBy() == null
                        ? null
                        : measurement.getTakenBy().getId(),
                measurement.isEditable(),
                measurement.getNote(),
                measurement.getAlterationNote(),
                measurement.getCreatedAt(),
                measurement.getUpdatedAt()
        );
    }
}