package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.worker.WorkerDtos.AllocateWorkRequestDto;
import com.sff.authapp.login.dtos.worker.WorkerDtos.WorkAllocationItemDto;
import com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerRequestDto;
import com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerResponseDto;

import java.math.BigDecimal;
import java.util.List;

public interface WorkerService {

    List<WorkerResponseDto> getAllWorkers(String keyword);

    WorkerResponseDto getWorkerById(Long workerId);

    WorkerResponseDto createWorker(WorkerRequestDto request);

    WorkerResponseDto updateWorker(Long workerId, WorkerRequestDto request);

    WorkerResponseDto updateWorkerCapacity(Long workerId, BigDecimal dailyCapacity);

    WorkerResponseDto updateWorkerStatus(Long workerId, boolean active);

    void deleteWorker(Long workerId);

    // ============================================================
    // WORK ALLOCATION & READY FOR DELIVERY CHECK
    // ============================================================

    List<WorkAllocationItemDto> getWorkAllocations(Long workerId, String status);

    List<WorkAllocationItemDto> getWorkAllocations(
            Long workerId,
            String status,
            Long serviceId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String stage
    );

    List<WorkAllocationItemDto> allocateWorkToWorker(AllocateWorkRequestDto request);

    WorkAllocationItemDto markAllocatedGarmentReady(Long garmentId, boolean ready);

    // ============================================================
    // WORKER ADVANCES & SETTLEMENTS (HISAB)
    // ============================================================

    List<com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerAdvanceDto> getWorkerAdvances(
            Long workerId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    );

    com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerAdvanceDto createWorkerAdvance(
            com.sff.authapp.login.dtos.worker.WorkerDtos.CreateWorkerAdvanceRequestDto request
    );

    void deleteWorkerAdvance(Long advanceId);

    List<com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerSettlementDto> getWorkerSettlements(Long workerId);

    com.sff.authapp.login.dtos.worker.WorkerDtos.WorkerSettlementDto createWorkerSettlement(
            com.sff.authapp.login.dtos.worker.WorkerDtos.CreateWorkerSettlementRequestDto request
    );
}
