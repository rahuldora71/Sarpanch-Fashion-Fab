package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.garmentmeasurement.CustomerMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementSummaryResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementUpdateRequestDto;
import com.sff.authapp.login.services.GarmentMeasurementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * REST controller for managing measurements attached to individual garments.
 *
 * <p>
 * A {@code GarmentMeasurement} represents the actual measurement values
 * taken for a specific garment inside an order.
 * </p>
 *
 * <p>
 * The measurement template comes from the {@code TailoringService}.
 * The actual measurement values are stored against the garment.
 * </p>
 *
 * <p>
 * Main responsibilities:
 * <ul>
 *     <li>Create garment measurements</li>
 *     <li>Retrieve measurements</li>
 *     <li>Retrieve customer measurement history</li>
 *     <li>Retrieve service-specific measurement history</li>
 *     <li>Retrieve latest measurements</li>
 *     <li>Provide measurement copy sources</li>
 *     <li>Update measurements</li>
 *     <li>Lock/unlock measurements</li>
 *     <li>Delete incorrect measurement records</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/garment-measurements")
@RequiredArgsConstructor
@Tag(
        name = "Garment Measurement Management",
        description = """
                APIs for managing measurements attached to individual garments,
                customer measurement history, latest measurements,
                measurement copying, updates, and locking.
                """
)
public class GarmentMeasurementController {


    private final GarmentMeasurementService garmentMeasurementService;


    // ============================================================
    // CREATE
    // ============================================================

    /**
     * Creates a measurement for a specific garment.
     *
     * <p>
     * The customer and tailoring service are derived from the garment.
     * The frontend therefore only needs to provide the garment ID and
     * measurement values.
     * </p>
     *
     * <p>
     * Measurement JSON keys must be valid Field IDs belonging to
     * the garment's tailoring service.
     * </p>
     *
     * @param request garment measurement creation request
     * @return newly created garment measurement
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Create garment measurement",
            description = """
                    Creates a new measurement record for a specific garment.

                    Measurement values must use Field IDs as JSON keys.

                    Example:

                    {
                        "garmentId": 10,
                        "measurementFields": {
                            "1": 38,
                            "2": 17,
                            "3": 24.5
                        }
                    }

                    The customer and tailoring service are automatically
                    derived from the garment.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Garment measurement created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid measurement data or measurement already exists"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Garment or measurement field not found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden"
            )
    })
    public ResponseEntity<GarmentMeasurementResponse> createMeasurement(
            @Valid
            @RequestBody
            GarmentMeasurementRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        garmentMeasurementService.createMeasurement(request)
                );
    }


    // ============================================================
    // CREATE CUSTOMER MEASUREMENT (DIRECT / STANDALONE)
    // ============================================================

    @PostMapping("/customer")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Create customer measurement directly",
            description = """
                    Creates a new measurement record for a customer
                    and tailoring service directly, without needing
                    an active order or garment.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Customer measurement created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid measurement data"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer or service not found"
            )
    })
    public ResponseEntity<GarmentMeasurementResponse> createCustomerMeasurement(
            @Valid
            @RequestBody
            CustomerMeasurementRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        garmentMeasurementService.createCustomerMeasurement(request)
                );
    }


    // ============================================================
    // GET ALL - PAGINATED WITH SEARCH
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get all garment measurements",
            description = """
                    Returns all garment measurements across all customers.
                    Results are pageable and can be filtered by keyword.
                    """
    )
    public ResponseEntity<Page<GarmentMeasurementResponse>> getAllMeasurements(
            @RequestParam(required = false) String keyword,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.getAllMeasurements(
                        keyword,
                        pageable
                )
        );
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    /**
     * Retrieves a garment measurement by its ID.
     *
     * @param measurementId measurement ID
     * @return garment measurement
     */
    @GetMapping("/{measurementId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get garment measurement by ID",
            description = """
                    Returns a single garment measurement using its
                    measurement ID.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Garment measurement found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Garment measurement not found"
            )
    })
    public ResponseEntity<GarmentMeasurementResponse> getMeasurementById(
            @PathVariable Long measurementId
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.getMeasurementById(
                        measurementId
                )
        );
    }


    // ============================================================
    // GET BY GARMENT
    // ============================================================

    /**
     * Retrieves the measurement associated with a garment.
     *
     * <p>
     * Since one garment has at most one GarmentMeasurement,
     * this endpoint returns a single measurement.
     * </p>
     *
     * @param garmentId garment ID
     * @return garment measurement
     */
    @GetMapping("/garment/{garmentId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get measurement for garment",
            description = """
                    Returns the measurement associated with a specific garment.

                    A garment can have only one measurement record.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Garment measurement found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Garment measurement not found"
            )
    })
    public ResponseEntity<GarmentMeasurementResponse>
    getMeasurementByGarment(
            @PathVariable Long garmentId
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.getMeasurementByGarment(
                        garmentId
                )
        );
    }


    // ============================================================
    // CUSTOMER MEASUREMENT HISTORY
    // ============================================================

    /**
     * Retrieves all garment measurements belonging to a customer.
     *
     * <p>
     * Results are pageable and can be sorted using Spring Pageable
     * parameters.
     * </p>
     *
     * @param userId customer/user ID
     * @param pageable pagination and sorting configuration
     * @return paginated customer measurement history
     */
    @GetMapping("/customer/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get customer garment measurement history",
            description = """
                    Returns all garment measurements belonging to a customer.

                    Results are pageable.

                    Example:

                    GET /api/v1/garment-measurements/customer/9?page=0&size=20
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer measurement history retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found"
            )
    })
    public ResponseEntity<Page<GarmentMeasurementResponse>>
    getCustomerMeasurements(

            @PathVariable Long userId,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.getCustomerMeasurements(
                        userId,
                        pageable
                )
        );
    }


    // ============================================================
    // CUSTOMER + SERVICE MEASUREMENT HISTORY
    // ============================================================

    /**
     * Retrieves all measurements for a customer for a specific
     * tailoring service.
     *
     * @param userId customer/user ID
     * @param serviceId tailoring service ID
     * @param pageable pagination and sorting configuration
     * @return paginated service-specific measurement history
     */
    @GetMapping(
            "/customer/{userId}/service/{serviceId}"
    )
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get customer measurements for a service",
            description = """
                    Returns all garment measurements belonging to a
                    customer for a particular tailoring service.

                    Example:

                    Customer = 9
                    Service = 4 (Pant)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Measurements retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer or service not found"
            )
    })
    public ResponseEntity<Page<GarmentMeasurementResponse>>
    getCustomerServiceMeasurements(

            @PathVariable Long userId,

            @PathVariable Long serviceId,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService
                        .getCustomerServiceMeasurements(
                                userId,
                                serviceId,
                                pageable
                        )
        );
    }


    // ============================================================
    // LATEST MEASUREMENT
    // ============================================================

    /**
     * Retrieves the latest measurement for a customer and service.
     *
     * <p>
     * This endpoint is particularly useful when creating a new garment.
     * The frontend can use the response to pre-populate the measurement
     * form.
     * </p>
     *
     * @param userId customer/user ID
     * @param serviceId tailoring service ID
     * @return latest measurement
     */
    @GetMapping(
            "/customer/{userId}/service/{serviceId}/latest"
    )
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get latest customer measurement",
            description = """
                    Returns the most recent garment measurement of a
                    customer for the specified tailoring service.

                    This endpoint can be used to automatically populate
                    the measurement form when creating a new garment.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Latest measurement found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No measurement found for customer and service"
            )
    })
    public ResponseEntity<GarmentMeasurementResponse>
    getLatestMeasurement(

            @PathVariable Long userId,

            @PathVariable Long serviceId
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.getLatestMeasurement(
                        userId,
                        serviceId
                )
        );
    }


    // ============================================================
    // LATEST MEASUREMENT PER SERVICE
    // ============================================================

    /**
     * Retrieves the latest measurement for every service for which
     * the customer has measurement history.
     *
     * <p>
     * Example:
     *
     * <pre>
     * Shirt    -> latest Shirt measurement
     * Coat     -> latest Coat measurement
     * Pant     -> latest Pant measurement
     * Jodhpuri -> latest Jodhpuri measurement
     * </pre>
     *
     * @param userId customer/user ID
     * @return latest measurement for each service
     */
    @GetMapping(
            "/customer/{userId}/latest-per-service"
    )
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get latest measurement for every service",
            description = """
                    Returns the latest garment measurement for every
                    tailoring service for which the customer has
                    measurement history.

                    This is useful for displaying measurement history
                    grouped by tailoring service.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Latest measurements retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found"
            )
    })
    public ResponseEntity<List<GarmentMeasurementSummaryResponse>>
    getLatestMeasurementsPerService(

            @PathVariable Long userId
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService
                        .getLatestMeasurementsPerService(userId)
        );
    }


    // ============================================================
    // COPY SOURCES
    // ============================================================

    /**
     * Retrieves possible measurement sources that can be used
     * when creating a measurement for another service.
     *
     * <p>
     * The target service itself is excluded from the result.
     * </p>
     *
     * <p>
     * Example:
     *
     * <pre>
     * Target service:
     *     Shirt
     *
     * Available copy sources:
     *     Coat
     *     Jodhpuri
     *     Pant
     * </pre>
     *
     * <p>
     * The frontend can compare Field IDs and copy only the
     * compatible fields.
     * </p>
     *
     * @param userId customer/user ID
     * @param serviceId target service ID
     * @return possible measurement copy sources
     */
    @GetMapping(
            "/customer/{userId}/service/{serviceId}/copy-sources"
    )
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get measurement copy sources",
            description = """
                    Returns the customer's latest measurements from
                    other tailoring services.

                    The target service is excluded.

                    The frontend can compare Field IDs and determine
                    which fields are compatible for copying.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Copy sources retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer or service not found"
            )
    })
    public ResponseEntity<List<GarmentMeasurementSummaryResponse>>
    getCopySources(

            @PathVariable Long userId,

            @PathVariable Long serviceId
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.getCopySources(
                        userId,
                        serviceId
                )
        );
    }


    // ============================================================
    // UPDATE
    // ============================================================

    /**
     * Updates an existing garment measurement.
     *
     * <p>
     * The measurement must be editable.
     * </p>
     *
     * <p>
     * Measurement Field IDs must belong to the tailoring service
     * associated with the garment.
     * </p>
     *
     * @param measurementId measurement ID
     * @param request measurement update request
     * @return updated garment measurement
     */
    @PatchMapping("/{measurementId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Update garment measurement",
            description = """
                    Updates an existing garment measurement.

                    The measurement must be editable.

                    Submitted Field IDs must belong to the tailoring
                    service associated with the garment.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Garment measurement updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid measurement data or measurement is locked"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Measurement or field not found"
            )
    })
    public ResponseEntity<GarmentMeasurementResponse>
    updateMeasurement(

            @PathVariable Long measurementId,

            @Valid
            @RequestBody
            GarmentMeasurementUpdateRequestDto request
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.updateMeasurement(
                        measurementId,
                        request
                )
        );
    }


    // ============================================================
    // UPDATE EDITABLE STATUS
    // ============================================================

    /**
     * Locks or unlocks a garment measurement.
     *
     * <p>
     * When editable is false, normal workers cannot modify the
     * measurement through the update endpoint.
     * </p>
     *
     * @param measurementId measurement ID
     * @param editable whether the measurement should remain editable
     * @return updated garment measurement
     */
    @PatchMapping("/{measurementId}/editable")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(
            summary = "Lock or unlock garment measurement",
            description = """
                    Controls whether a garment measurement can be edited.

                    editable=true:
                    Measurement remains editable.

                    editable=false:
                    Measurement becomes locked and cannot be modified
                    through the normal update endpoint.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Editable status updated successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Garment measurement not found"
            )
    })
    public ResponseEntity<GarmentMeasurementResponse>
    updateEditableStatus(

            @PathVariable Long measurementId,

            @RequestParam boolean editable
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService.updateEditableStatus(
                        measurementId,
                        editable
                )
        );
    }


    // ============================================================
    // DELETE
    // ============================================================

    /**
     * Permanently deletes a garment measurement.
     *
     * <p>
     * This should generally be restricted to administrators or
     * owners because deleting measurement history is destructive.
     * </p>
     *
     * @param measurementId measurement ID
     * @return HTTP 204 when successfully deleted
     */
    @DeleteMapping("/{measurementId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(
            summary = "Delete garment measurement",
            description = """
                    Permanently deletes a garment measurement.

                    Use this only when removing incorrect or unwanted
                    measurement history.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Garment measurement deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Garment measurement not found"
            )
    })
    public ResponseEntity<Void> deleteMeasurement(

            @PathVariable Long measurementId
    ) {

        garmentMeasurementService.deleteMeasurement(
                measurementId
        );

        return ResponseEntity.noContent().build();
    }
}