package com.sff.authapp.login.dtos.fabric;

import com.sff.authapp.login.entities.FabricSample;

import java.time.LocalDateTime;

public record FabricSampleResponse(

        Long id,

        String sampleCode,

        String bookNumber,

        String pageNumber,

        String color,

        String description,

        Long customerId,

        String customerName,

        Long orderId,

        String orderNumber,

        FabricSample.Status status,

        LocalDateTime createdAt
) {
}