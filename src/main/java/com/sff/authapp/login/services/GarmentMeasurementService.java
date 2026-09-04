package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementUpdateRequestDto;

public interface GarmentMeasurementService {

    GarmentMeasurementResponse createMeasurement(
            GarmentMeasurementRequestDto request
    );

    GarmentMeasurementResponse getMeasurementById(
            Long measurementId
    );

    GarmentMeasurementResponse getMeasurementByGarment(
            Long garmentId
    );

    GarmentMeasurementResponse updateMeasurement(
            Long measurementId,
            GarmentMeasurementUpdateRequestDto request
    );

    GarmentMeasurementResponse updateEditableStatus(
            Long measurementId,
            boolean editable
    );

    void deleteMeasurement(
            Long measurementId
    );
}