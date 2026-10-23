package com.sff.authapp.login.dtos.customer;

import java.time.LocalDateTime;
import java.util.Map;

public record CustomerProfileMeasurement(

        Long measurementId,

        Long garmentId,

        Long orderId,

        Long serviceId,

        String serviceName,

        Map<String, Object> measurementFields,

        LocalDateTime measuredAt

) {
}