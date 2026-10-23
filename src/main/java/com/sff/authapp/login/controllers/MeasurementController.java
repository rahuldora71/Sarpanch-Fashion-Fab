//package com.sff.authapp.login.controllers;
//
//import com.sff.authapp.login.dtos.measurement.MeasurementRequestDto;
//import com.sff.authapp.login.dtos.measurement.MeasurementResponse;
//import com.sff.authapp.login.dtos.garmentmeasurement.MeasurementSummaryResponse;
//import com.sff.authapp.login.dtos.measurement.MeasurementUpdateRequestDto;
//import com.sff.authapp.login.services.MeasurementService;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//
//import jakarta.validation.Valid;
//
//import lombok.RequiredArgsConstructor;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableDefault;
//
//import org.springframework.http.ResponseEntity;
//
//import org.springframework.security.access.prepost.PreAuthorize;
//
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/measurements")
//@RequiredArgsConstructor
//@Tag(
//        name = "Measurement Management",
//        description = """
//                APIs for managing customer measurement history,
//                latest measurements, measurement copying,
//                and measurement updates.
//                """
//)
//public class MeasurementController {
//
//    private final MeasurementService measurementService;
//
//
//    // ============================================================
//    // CREATE
//    // ============================================================
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Create customer measurement",
//            description = """
//                    Creates a new measurement record for a customer
//                    and a specific tailoring service.
//
//                    Measurement values must use Field IDs as JSON keys.
//
//                    Example:
//
//                    {
//                        "1": 38,
//                        "2": 17,
//                        "3": 24.5
//                    }
//
//                    Where the numbers represent Field IDs.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Measurement created successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "Invalid measurement data"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Customer, service, or field not found"
//            ),
//            @ApiResponse(
//                    responseCode = "401",
//                    description = "Unauthorized"
//            ),
//            @ApiResponse(
//                    responseCode = "403",
//                    description = "Forbidden"
//            )
//    })
//    public ResponseEntity<MeasurementResponse> createMeasurement(
//            @Valid
//            @RequestBody
//            MeasurementRequestDto request
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService.createMeasurement(
//                        request
//                )
//        );
//    }
//
//
//    // ============================================================
//    // GET BY ID
//    // ============================================================
//
//    @GetMapping("/{measurementId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Get measurement by ID",
//            description = """
//                    Returns a single measurement record
//                    using its measurement ID.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Measurement found"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Measurement not found"
//            )
//    })
//    public ResponseEntity<MeasurementResponse> getMeasurementById(
//
//            @PathVariable
//            Long measurementId
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService.getMeasurementById(
//                        measurementId
//                )
//        );
//    }
//
//
//    // ============================================================
//    // CUSTOMER MEASUREMENT HISTORY
//    // ============================================================
//
//    @GetMapping("/customer/{userId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Get customer measurement history",
//            description = """
//                    Returns the measurement history of a customer.
//
//                    Results are pageable and can be sorted
//                    using Spring Pageable parameters.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Measurement history retrieved successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Customer not found"
//            )
//    })
//    public ResponseEntity<Page<MeasurementResponse>>
//    getCustomerMeasurements(
//
//            @PathVariable
//            Long userId,
//
//            @PageableDefault(
//                    size = 20,
//                    sort = "createdAt"
//            )
//            Pageable pageable
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService.getCustomerMeasurements(
//                        userId,
//                        pageable
//                )
//        );
//    }
//
//
//    // ============================================================
//    // CUSTOMER + SERVICE MEASUREMENT HISTORY
//    // ============================================================
//
//    @GetMapping(
//            "/customer/{userId}/service/{serviceId}"
//    )
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Get customer measurements for a service",
//            description = """
//                    Returns all measurements belonging to a customer
//                    for a particular tailoring service.
//
//                    Example:
//
//                    Customer = 15
//                    Service = 1 (Shirt)
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Measurements retrieved successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Customer or service not found"
//            )
//    })
//    public ResponseEntity<Page<MeasurementResponse>>
//    getCustomerServiceMeasurements(
//
//            @PathVariable
//            Long userId,
//
//            @PathVariable
//            Long serviceId,
//
//            @PageableDefault(
//                    size = 20,
//                    sort = "createdAt"
//            )
//            Pageable pageable
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService
//                        .getCustomerServiceMeasurements(
//                                userId,
//                                serviceId,
//                                pageable
//                        )
//        );
//    }
//
//
//    // ============================================================
//    // LATEST MEASUREMENT
//    // ============================================================
//
//    @GetMapping(
//            "/customer/{userId}/service/{serviceId}/latest"
//    )
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Get latest customer measurement",
//            description = """
//                    Returns the most recent measurement of a customer
//                    for the specified tailoring service.
//
//                    This endpoint is useful when creating a new garment.
//
//                    If the customer has previously been measured for
//                    this service, the frontend can use the response
//                    to automatically populate the measurement form.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Latest measurement found"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "No measurement found for this customer and service"
//            )
//    })
//    public ResponseEntity<MeasurementResponse>
//    getLatestMeasurement(
//
//            @PathVariable
//            Long userId,
//
//            @PathVariable
//            Long serviceId
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService.getLatestMeasurement(
//                        userId,
//                        serviceId
//                )
//        );
//    }
//
//
//    // ============================================================
//    // LATEST MEASUREMENT PER SERVICE
//    // ============================================================
//
//    @GetMapping(
//            "/customer/{userId}/latest-per-service"
//    )
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Get latest measurement for every service",
//            description = """
//                    Returns the latest measurement for every
//                    tailoring service for which the customer
//                    has measurement history.
//
//                    Example:
//
//                    Shirt     → latest Shirt measurement
//                    Coat      → latest Coat measurement
//                    Pant      → latest Pant measurement
//                    Jodhpuri  → latest Jodhpuri measurement
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Latest measurements retrieved successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Customer not found"
//            )
//    })
//    public ResponseEntity<List<MeasurementSummaryResponse>>
//    getLatestMeasurementsPerService(
//
//            @PathVariable
//            Long userId
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService
//                        .getLatestMeasurementsPerService(
//                                userId
//                        )
//        );
//    }
//
//
//    // ============================================================
//    // COPY SOURCES
//    // ============================================================
//
//    @GetMapping(
//            "/customer/{userId}/service/{serviceId}/copy-sources"
//    )
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Get measurement copy sources",
//            description = """
//                    Returns the customer's latest measurements
//                    from other tailoring services.
//
//                    This endpoint is useful when the customer does
//                    not have an existing measurement for the target
//                    service.
//
//                    Example:
//
//                    Target service:
//                    Shirt
//
//                    Existing measurements:
//                    Coat
//                    Jodhpuri
//                    Pant
//
//                    The frontend can compare Field IDs and identify
//                    common fields such as Chest, Shoulder, Sleeve,
//                    etc.
//
//                    Measurement values are stored using Field IDs.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Copy sources retrieved successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Customer or service not found"
//            )
//    })
//    public ResponseEntity<List<MeasurementSummaryResponse>>
//    getCopySources(
//
//            @PathVariable
//            Long userId,
//
//            @PathVariable
//            Long serviceId
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService.getCopySources(
//                        userId,
//                        serviceId
//                )
//        );
//    }
//
//
//    // ============================================================
//    // UPDATE
//    // ============================================================
//
//    @PatchMapping("/{measurementId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    @Operation(
//            summary = "Update measurement",
//            description = """
//                    Updates an existing measurement.
//
//                    The measurement must be editable.
//
//                    Field IDs must belong to the service associated
//                    with the measurement.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Measurement updated successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "Invalid measurement data or measurement is locked"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Measurement or field not found"
//            )
//    })
//    public ResponseEntity<MeasurementResponse>
//    updateMeasurement(
//
//            @PathVariable
//            Long measurementId,
//
//            @Valid
//            @RequestBody
//            MeasurementUpdateRequestDto request
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService.updateMeasurement(
//                        measurementId,
//                        request
//                )
//        );
//    }
//
//
//    // ============================================================
//    // UPDATE EDITABLE STATUS
//    // ============================================================
//
//    @PatchMapping("/{measurementId}/editable")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
//    @Operation(
//            summary = "Update measurement editable status",
//            description = """
//                    Locks or unlocks a measurement.
//
//                    editable=true:
//                    Measurement can be modified.
//
//                    editable=false:
//                    Measurement becomes locked and cannot be modified.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Editable status updated successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Measurement not found"
//            )
//    })
//    public ResponseEntity<MeasurementResponse>
//    updateEditableStatus(
//
//            @PathVariable
//            Long measurementId,
//
//            @RequestParam
//            boolean editable
//
//    ) {
//
//        return ResponseEntity.ok(
//                measurementService.updateEditableStatus(
//                        measurementId,
//                        editable
//                )
//        );
//    }
//
//
//    // ============================================================
//    // DELETE
//    // ============================================================
//
//    @DeleteMapping("/{measurementId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
//    @Operation(
//            summary = "Delete measurement",
//            description = """
//                    Permanently deletes a measurement record.
//
//                    Use this only for removing incorrect or
//                    unwanted measurement history.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "204",
//                    description = "Measurement deleted successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Measurement not found"
//            )
//    })
//    public ResponseEntity<Void> deleteMeasurement(
//
//            @PathVariable
//            Long measurementId
//
//    ) {
//
//        measurementService.deleteMeasurement(
//                measurementId
//        );
//
//        return ResponseEntity.noContent().build();
//    }
//}