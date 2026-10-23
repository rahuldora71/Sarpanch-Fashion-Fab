package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.alteration.AlterationBatchRequestDto;
import com.sff.authapp.login.dtos.alteration.AlterationBatchResponseDto;
import com.sff.authapp.login.dtos.alteration.AlterationQueueItemDto;
import com.sff.authapp.login.dtos.alteration.AlterationQueueSummaryDto;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.entities.GarmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AlterationService {

    AlterationBatchResponseDto processAlterations(AlterationBatchRequestDto request);

    Page<GarmentResponse> getActiveAlterations(Pageable pageable);

    Page<AlterationQueueItemDto> getAlterationQueue(
            GarmentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    );

    AlterationQueueSummaryDto getAlterationQueueSummary();

    GarmentResponse quickDeliverAlteration(Long garmentId, String note);
}
