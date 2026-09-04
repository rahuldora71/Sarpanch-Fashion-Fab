package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.garment.GarmentRequestDto;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.dtos.garment.GarmentUpdateRequestDto;
import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.services.GarmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/garments")
@RequiredArgsConstructor
@Tag(
        name = "Garment Management",
        description = "APIs for managing garments belonging to customer orders"
)
public class GarmentController {

    private final GarmentService garmentService;


    @PostMapping
    @Operation(
            summary = "Create garment"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentResponse> createGarment(
            @Valid @RequestBody GarmentRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        garmentService.createGarment(request)
                );
    }


    @GetMapping("/{garmentId}")
    @Operation(
            summary = "Get garment by ID"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentResponse> getGarmentById(
            @PathVariable Long garmentId
    ) {

        return ResponseEntity.ok(
                garmentService.getGarmentById(garmentId)
        );
    }


    @GetMapping("/order/{orderId}")
    @Operation(
            summary = "Get all garments for an order"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<List<GarmentResponse>> getGarmentsByOrder(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                garmentService.getGarmentsByOrder(orderId)
        );
    }


    @GetMapping("/order/{orderId}/page")
    @Operation(
            summary = "Get paginated garments for an order"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<Page<GarmentResponse>> getGarmentsByOrder(
            @PathVariable Long orderId,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                garmentService.getGarmentsByOrder(
                        orderId,
                        pageable
                )
        );
    }


    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get garments by status"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<Page<GarmentResponse>> getGarmentsByStatus(
            @PathVariable GarmentStatus status,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                garmentService.getGarmentsByStatus(
                        status,
                        pageable
                )
        );
    }


    @GetMapping("/service/{serviceId}")
    @Operation(
            summary = "Get garments by service"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<Page<GarmentResponse>> getGarmentsByService(
            @PathVariable Long serviceId,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                garmentService.getGarmentsByService(
                        serviceId,
                        pageable
                )
        );
    }


    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Get customer garments"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<Page<GarmentResponse>> getCustomerGarments(
            @PathVariable Long customerId,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                garmentService.getCustomerGarments(
                        customerId,
                        pageable
                )
        );
    }


    @PatchMapping("/{garmentId}")
    @Operation(
            summary = "Update garment"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentResponse> updateGarment(
            @PathVariable Long garmentId,
            @Valid @RequestBody GarmentUpdateRequestDto request
    ) {

        return ResponseEntity.ok(
                garmentService.updateGarment(
                        garmentId,
                        request
                )
        );
    }


    @PatchMapping("/{garmentId}/status")
    @Operation(
            summary = "Update garment status"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentResponse> updateStatus(
            @PathVariable Long garmentId,
            @RequestParam GarmentStatus status
    ) {

        return ResponseEntity.ok(
                garmentService.updateStatus(
                        garmentId,
                        status
                )
        );
    }


    @DeleteMapping("/{garmentId}")
    @Operation(
            summary = "Delete garment"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Void> deleteGarment(
            @PathVariable Long garmentId
    ) {

        garmentService.deleteGarment(garmentId);

        return ResponseEntity.noContent().build();
    }
}