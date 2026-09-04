package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementRequestDto;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementResponse;
import com.sff.authapp.login.dtos.garmentmeasurement.GarmentMeasurementUpdateRequestDto;
import com.sff.authapp.login.services.GarmentMeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/garment-measurements")
@RequiredArgsConstructor
@Tag(
        name = "Garment Measurement Management",
        description = "APIs for measurements attached to individual garments"
)
public class GarmentMeasurementController {

    private final GarmentMeasurementService garmentMeasurementService;


    @PostMapping
    @Operation(
            summary = "Create garment measurement"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentMeasurementResponse> createMeasurement(
            @Valid @RequestBody GarmentMeasurementRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        garmentMeasurementService
                                .createMeasurement(request)
                );
    }


    @GetMapping("/{measurementId}")
    @Operation(
            summary = "Get garment measurement"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentMeasurementResponse> getMeasurementById(
            @PathVariable Long measurementId
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService
                        .getMeasurementById(measurementId)
        );
    }


    @GetMapping("/garment/{garmentId}")
    @Operation(
            summary = "Get measurement for garment"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentMeasurementResponse> getMeasurementByGarment(
            @PathVariable Long garmentId
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService
                        .getMeasurementByGarment(garmentId)
        );
    }


    @PatchMapping("/{measurementId}")
    @Operation(
            summary = "Update garment measurement"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentMeasurementResponse> updateMeasurement(
            @PathVariable Long measurementId,
            @Valid @RequestBody
            GarmentMeasurementUpdateRequestDto request
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService
                        .updateMeasurement(
                                measurementId,
                                request
                        )
        );
    }


    @PatchMapping("/{measurementId}/editable")
    @Operation(
            summary = "Lock or unlock garment measurement"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<GarmentMeasurementResponse>
    updateEditableStatus(
            @PathVariable Long measurementId,
            @RequestParam boolean editable
    ) {

        return ResponseEntity.ok(
                garmentMeasurementService
                        .updateEditableStatus(
                                measurementId,
                                editable
                        )
        );
    }


    @DeleteMapping("/{measurementId}")
    @Operation(
            summary = "Delete garment measurement"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Void> deleteMeasurement(
            @PathVariable Long measurementId
    ) {

        garmentMeasurementService.deleteMeasurement(
                measurementId
        );

        return ResponseEntity.noContent().build();
    }
}