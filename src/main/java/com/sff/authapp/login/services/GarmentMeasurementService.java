package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.garmentmeasurement.CustomerMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementSummaryResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementUpdateRequestDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GarmentMeasurementService {

    // Create for garment
    GarmentMeasurementResponse createMeasurement(
            GarmentMeasurementRequestDto request
    );

    // Create directly for customer (without garment/order)
    GarmentMeasurementResponse createCustomerMeasurement(
            CustomerMeasurementRequestDto request
    );

    // Get all measurements paginated with optional search
    Page<GarmentMeasurementResponse> getAllMeasurements(
            String keyword,
            Pageable pageable
    );

    // Read
    GarmentMeasurementResponse getMeasurementById(
            Long measurementId
    );

    GarmentMeasurementResponse getMeasurementByGarment(
            Long garmentId
    );

    // Customer history
    Page<GarmentMeasurementResponse> getCustomerMeasurements(
            Long userId,
            Pageable pageable
    );

    // Customer + service history
    Page<GarmentMeasurementResponse> getCustomerServiceMeasurements(
            Long userId,
            Long serviceId,
            Pageable pageable
    );

    // Latest measurement for customer + service
    GarmentMeasurementResponse getLatestMeasurement(
            Long userId,
            Long serviceId
    );

    // Latest measurement for every service
    List<GarmentMeasurementSummaryResponse>
    getLatestMeasurementsPerService(
            Long userId
    );

    // Measurement copy sources
    List<GarmentMeasurementSummaryResponse> getCopySources(
            Long userId,
            Long targetServiceId
    );

    // Update
    GarmentMeasurementResponse updateMeasurement(
            Long measurementId,
            GarmentMeasurementUpdateRequestDto request
    );

    // Lock / unlock
    GarmentMeasurementResponse updateEditableStatus(
            Long measurementId,
            boolean editable
    );

    // Delete
    void deleteMeasurement(
            Long measurementId
    );
}