package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.alteration.AlterationBatchRequestDto;
import com.sff.authapp.login.dtos.alteration.AlterationBatchResponseDto;
import com.sff.authapp.login.dtos.alteration.AlterationQueueItemDto;
import com.sff.authapp.login.dtos.alteration.AlterationQueueSummaryDto;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.services.AlterationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/alterations")
@RequiredArgsConstructor
@Tag(
        name = "Alteration Management",
        description = "APIs for recording alterations, batch field adjustments, alteration queue, and delivery"
)
public class AlterationController {

    private final AlterationService alterationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    @Operation(
            summary = "Process batch alterations",
            description = "Marks garments for alteration, applies measurement adjustments, and optionally updates customer default measurements for future orders."
    )
    public ResponseEntity<AlterationBatchResponseDto> processAlterations(
            @Valid @RequestBody AlterationBatchRequestDto request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(alterationService.processAlterations(request));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    @Operation(
            summary = "Get active alterations",
            description = "Returns paginated garments currently in ALTERATION status."
    )
    public ResponseEntity<Page<GarmentResponse>> getActiveAlterations(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(alterationService.getActiveAlterations(pageable));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    @Operation(
            summary = "Get alteration queue with rich customer & order details",
            description = "Returns date-wise or filtered queue of garments in alteration with customer, order, and adjustment details."
    )
    public ResponseEntity<Page<AlterationQueueItemDto>> getAlterationQueue(
            @RequestParam(required = false, defaultValue = "ALTERATION") GarmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(alterationService.getAlterationQueue(status, startDate, endDate, search, pageable));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    @Operation(
            summary = "Get alteration queue counts and summary",
            description = "Returns counts for overdue, today, tomorrow, upcoming, and delivered alterations."
    )
    public ResponseEntity<AlterationQueueSummaryDto> getAlterationQueueSummary() {
        return ResponseEntity.ok(alterationService.getAlterationQueueSummary());
    }

    @PostMapping("/{garmentId}/deliver")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','WORKER')")
    @Operation(
            summary = "Quick deliver altered garment",
            description = "Marks an altered garment as DELIVERED and records who delivered it."
    )
    public ResponseEntity<GarmentResponse> quickDeliverAlteration(
            @PathVariable Long garmentId,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(alterationService.quickDeliverAlteration(garmentId, note));
    }
}
