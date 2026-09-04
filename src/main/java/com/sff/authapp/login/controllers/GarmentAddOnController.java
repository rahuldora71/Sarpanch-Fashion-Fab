package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.garmentaddon.GarmentAddOnRequestDto;
import com.sff.authapp.login.dtos.garmentaddon.GarmentAddOnResponse;
import com.sff.authapp.login.services.GarmentAddOnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/garment-addons")
@RequiredArgsConstructor
@Tag(
        name = "Garment Add-On Management",
        description = "APIs for managing add-ons attached to garments"
)
public class GarmentAddOnController {

    private final GarmentAddOnService garmentAddOnService;


    @PostMapping
    @Operation(
            summary = "Add add-on to garment"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentAddOnResponse> addAddOn(
            @Valid @RequestBody GarmentAddOnRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        garmentAddOnService.addAddOn(
                                request
                        )
                );
    }


    @GetMapping("/{garmentAddOnId}")
    @Operation(
            summary = "Get garment add-on"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentAddOnResponse> getById(
            @PathVariable Long garmentAddOnId
    ) {

        return ResponseEntity.ok(
                garmentAddOnService.getById(
                        garmentAddOnId
                )
        );
    }


    @GetMapping("/garment/{garmentId}")
    @Operation(
            summary = "Get add-ons for garment"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<List<GarmentAddOnResponse>> getByGarment(
            @PathVariable Long garmentId
    ) {

        return ResponseEntity.ok(
                garmentAddOnService.getByGarment(
                        garmentId
                )
        );
    }


    @PatchMapping("/{garmentAddOnId}/quantity")
    @Operation(
            summary = "Update garment add-on quantity"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<GarmentAddOnResponse> updateQuantity(
            @PathVariable Long garmentAddOnId,
            @RequestParam Integer quantity
    ) {

        return ResponseEntity.ok(
                garmentAddOnService.updateQuantity(
                        garmentAddOnId,
                        quantity
                )
        );
    }


    @DeleteMapping("/{garmentAddOnId}")
    @Operation(
            summary = "Remove add-on from garment"
    )
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long garmentAddOnId
    ) {

        garmentAddOnService.delete(
                garmentAddOnId
        );

        return ResponseEntity.noContent().build();
    }
}