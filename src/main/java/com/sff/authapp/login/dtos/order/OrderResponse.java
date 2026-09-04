package com.sff.authapp.login.dtos.order;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderResponse(

        Long id,

        Long customerId,

        String customerName,

        String customerCode,

        LocalDate orderDate,

        String sampleBookPageNumber,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}