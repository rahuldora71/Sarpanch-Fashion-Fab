package com.sff.authapp.login.dtos.alteration;

import java.util.List;

public record AlterationBatchResponseDto(
        Long orderId,
        Long customerId,
        int alteredGarmentsCount,
        boolean customerDefaultUpdated,
        List<Long> alteredGarmentIds,
        String message
) {
}
