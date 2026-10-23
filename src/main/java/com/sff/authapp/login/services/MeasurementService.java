//package com.sff.authapp.login.services;
//
//import com.sff.authapp.login.dtos.measurement.MeasurementRequestDto;
//import com.sff.authapp.login.dtos.measurement.MeasurementResponse;
//import com.sff.authapp.login.dtos.garmentmeasurement.MeasurementSummaryResponse;
//import com.sff.authapp.login.dtos.measurement.MeasurementUpdateRequestDto;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//
//public interface MeasurementService {
//
//    // ============================================================
//    // CREATE
//    // ============================================================
//
//    /**
//     * Creates a new customer measurement for a specific
//     * tailoring service.
//     *
//     * Measurement values must use Field IDs as keys.
//     *
//     * Example:
//     *
//     * {
//     *     "1": 38,
//     *     "2": 17,
//     *     "3": 24.5
//     * }
//     */
//    MeasurementResponse createMeasurement(
//            MeasurementRequestDto request
//    );
//
//
//    // ============================================================
//    // GET BY ID
//    // ============================================================
//
//    /**
//     * Returns a single measurement by its ID.
//     */
//    MeasurementResponse getMeasurementById(
//            Long measurementId
//    );
//
//
//    // ============================================================
//    // CUSTOMER MEASUREMENT HISTORY
//    // ============================================================
//
//    /**
//     * Returns all measurement records belonging to a customer.
//     *
//     * Results are pageable.
//     */
//    Page<MeasurementResponse> getCustomerMeasurements(
//            Long userId,
//            Pageable pageable
//    );
//
//
//    // ============================================================
//    // CUSTOMER + SERVICE MEASUREMENT HISTORY
//    // ============================================================
//
//    /**
//     * Returns measurement history for a customer
//     * for a specific tailoring service.
//     *
//     * Example:
//     *
//     * Customer = 15
//     * Service  = Shirt
//     */
//    Page<MeasurementResponse> getCustomerServiceMeasurements(
//            Long userId,
//            Long serviceId,
//            Pageable pageable
//    );
//
//
//    // ============================================================
//    // LATEST MEASUREMENT
//    // ============================================================
//
//    /**
//     * Returns the latest measurement of a customer
//     * for the specified tailoring service.
//     *
//     * Used when creating a new garment.
//     *
//     * Example:
//     *
//     * Customer = Rahul
//     * Service  = Shirt
//     *
//     * Returns the latest Shirt measurement.
//     */
//    MeasurementResponse getLatestMeasurement(
//            Long userId,
//            Long serviceId
//    );
//
//
//    // ============================================================
//    // LATEST MEASUREMENT PER SERVICE
//    // ============================================================
//
//    /**
//     * Returns the latest measurement for every service
//     * for which the customer has measurement history.
//     *
//     * Example:
//     *
//     * Shirt    -> latest Shirt measurement
//     * Coat     -> latest Coat measurement
//     * Pant     -> latest Pant measurement
//     * Jodhpuri -> latest Jodhpuri measurement
//     */
//    List<MeasurementSummaryResponse>
//    getLatestMeasurementsPerService(
//            Long userId
//    );
//
//
//    // ============================================================
//    // COPY SOURCES
//    // ============================================================
//
//    /**
//     * Returns the latest measurements from other services
//     * that can potentially be used as copy sources.
//     *
//     * Example:
//     *
//     * Target service = Shirt
//     *
//     * Available sources:
//     *
//     * Coat
//     * Jodhpuri
//     * Pant
//     *
//     * The frontend can compare Field IDs between the target
//     * service and these source measurements and allow the user
//     * to copy common fields.
//     */
//    List<MeasurementSummaryResponse>
//    getCopySources(
//            Long userId,
//            Long targetServiceId
//    );
//
//
//    // ============================================================
//    // UPDATE
//    // ============================================================
//
//    /**
//     * Updates an existing customer measurement.
//     *
//     * The measurement must be editable.
//     *
//     * Field IDs must:
//     *
//     * 1. Exist in the Field master table.
//     * 2. Belong to the service associated with the measurement.
//     * 3. Satisfy required-field validation.
//     */
//    MeasurementResponse updateMeasurement(
//            Long measurementId,
//            MeasurementUpdateRequestDto request
//    );
//
//
//    // ============================================================
//    // EDITABLE / LOCK
//    // ============================================================
//
//    /**
//     * Locks or unlocks a measurement.
//     *
//     * editable = true
//     *     Measurement can be modified.
//     *
//     * editable = false
//     *     Measurement is locked.
//     */
//    MeasurementResponse updateEditableStatus(
//            Long measurementId,
//            boolean editable
//    );
//
//
//    // ============================================================
//    // DELETE
//    // ============================================================
//
//    /**
//     * Permanently deletes a measurement record.
//     */
//    void deleteMeasurement(
//            Long measurementId
//    );
//}