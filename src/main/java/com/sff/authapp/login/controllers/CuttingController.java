package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.cutting.CuttingQueueResponse;
import com.sff.authapp.login.dtos.cutting.CuttingWorkspaceResponse;
import com.sff.authapp.login.dtos.cutting.MarkAsCutRequest;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.services.CuttingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/cutting")
@RequiredArgsConstructor
@Tag(
        name = "Cutting Queue Management",
        description = "APIs for cutter workers to view pending garments by delivery date and mark them as cut"
)
public class CuttingController {

    private final CuttingService cuttingService;

    // ============================================================
    // GET CUTTING QUEUE
    // ============================================================

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get cutting queue",
            description = "Returns all garments waiting to be cut, grouped by delivery date and order"
    )
    public ResponseEntity<CuttingQueueResponse> getCuttingQueue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(cuttingService.getCuttingQueueByDateRange(startDate, endDate));
        }
        return ResponseEntity.ok(cuttingService.getCuttingQueue());
    }

    // ============================================================
    // GET CUTTING WORKSPACE FOR AN ORDER
    // ============================================================

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get cutting workspace for an order",
            description = "Returns order with all garments, measurements, and add-on cutting instructions"
    )
    public ResponseEntity<CuttingWorkspaceResponse> getCuttingWorkspace(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(cuttingService.getCuttingWorkspace(orderId));
    }

    // ============================================================
    // MARK GARMENT AS CUT
    // ============================================================

    @PatchMapping("/garments/{garmentId}/cut")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Mark garment as cut",
            description = "Updates garment status from FABRIC_CUT to CUTTING/STITCHING and records cutter worker"
    )
    public ResponseEntity<GarmentResponse> markAsCut(
            @PathVariable Long garmentId,
            @RequestBody(required = false) MarkAsCutRequest request,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);
        return ResponseEntity.ok(cuttingService.markAsCut(garmentId, request, authenticatedUserId));
    }

    // ============================================================
    // CANCEL / REVERT CUT
    // ============================================================

    @PatchMapping("/garments/{garmentId}/revert-cut")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Revert garment cut status",
            description = "Reverts garment status back to FABRIC_CUT"
    )
    public ResponseEntity<GarmentResponse> cancelCut(
            @PathVariable Long garmentId,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);
        return ResponseEntity.ok(cuttingService.cancelCut(garmentId, authenticatedUserId));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
