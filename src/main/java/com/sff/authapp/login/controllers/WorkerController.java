package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.worker.WorkerDtos.AllocateWorkRequestDto;
import com.sff.authapp.login.dtos.worker.WorkerDtos.WorkAllocationItemDto;
import com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerRequestDto;
import com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerResponseDto;
import com.sff.authapp.login.services.WorkerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/workers", "/api/v1/api/v1/workers"})
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<List<WorkerResponseDto>> getAllWorkers(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(workerService.getAllWorkers(keyword));
    }

    @GetMapping("/{workerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<WorkerResponseDto> getWorkerById(@PathVariable Long workerId) {
        return ResponseEntity.ok(workerService.getWorkerById(workerId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<WorkerResponseDto> createWorker(
            @Valid @RequestBody WorkerRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workerService.createWorker(request));
    }

    @PutMapping("/{workerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<WorkerResponseDto> updateWorker(
            @PathVariable Long workerId,
            @Valid @RequestBody WorkerRequestDto request
    ) {
        return ResponseEntity.ok(workerService.updateWorker(workerId, request));
    }

    @PatchMapping("/{workerId}/capacity")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<WorkerResponseDto> updateWorkerCapacity(
            @PathVariable Long workerId,
            @RequestParam BigDecimal dailyCapacity
    ) {
        return ResponseEntity.ok(workerService.updateWorkerCapacity(workerId, dailyCapacity));
    }

    @PatchMapping("/{workerId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<WorkerResponseDto> updateWorkerStatus(
            @PathVariable Long workerId,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(workerService.updateWorkerStatus(workerId, active));
    }

    @DeleteMapping("/{workerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER')")
    public ResponseEntity<Void> deleteWorker(@PathVariable Long workerId) {
        workerService.deleteWorker(workerId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // WORK ALLOCATION & READY FOR DELIVERY CHECK ENDPOINTS
    // ============================================================

    @GetMapping("/allocations")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<List<WorkAllocationItemDto>> getWorkAllocations(
            @RequestParam(required = false) Long workerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) String stage
    ) {
        return ResponseEntity.ok(workerService.getWorkAllocations(workerId, status, serviceId, startDate, endDate, stage));
    }

    @PostMapping("/allocations/assign")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<List<WorkAllocationItemDto>> allocateWorkToWorker(
            @Valid @RequestBody AllocateWorkRequestDto request
    ) {
        return ResponseEntity.ok(workerService.allocateWorkToWorker(request));
    }

    @PatchMapping("/allocations/{garmentId}/ready")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<WorkAllocationItemDto> markAllocatedGarmentReady(
            @PathVariable Long garmentId,
            @RequestParam(defaultValue = "true") boolean ready
    ) {
        return ResponseEntity.ok(workerService.markAllocatedGarmentReady(garmentId, ready));
    }

    // ============================================================
    // WORKER ADVANCES & SETTLEMENTS (HISAB)
    // ============================================================

    @GetMapping("/{workerId}/advances")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<List<com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerAdvanceDto>> getWorkerAdvances(
            @PathVariable Long workerId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        return ResponseEntity.ok(workerService.getWorkerAdvances(workerId, startDate, endDate));
    }

    @PostMapping("/advances")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerAdvanceDto> createWorkerAdvance(
            @Valid @RequestBody com.sff.authapp.login.dtos.worker.WorkerDtos.CreateWorkerAdvanceRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workerService.createWorkerAdvance(request));
    }

    @DeleteMapping("/advances/{advanceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER')")
    public ResponseEntity<Void> deleteWorkerAdvance(@PathVariable Long advanceId) {
        workerService.deleteWorkerAdvance(advanceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workerId}/settlements")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<List<com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerSettlementDto>> getWorkerSettlements(
            @PathVariable Long workerId
    ) {
        return ResponseEntity.ok(workerService.getWorkerSettlements(workerId));
    }

    @PostMapping("/settlements")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER', 'SHOPKEEPER', 'TAILOR')")
    public ResponseEntity<com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerSettlementDto> createWorkerSettlement(
            @Valid @RequestBody com.sff.authapp.login.dtos.worker.WorkerDtos.CreateWorkerSettlementRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workerService.createWorkerSettlement(request));
    }
}
