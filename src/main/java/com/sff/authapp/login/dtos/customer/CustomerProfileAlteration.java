package com.sff.authapp.login.dtos.customer;

import java.time.LocalDate;

public record CustomerProfileAlteration(
        Long id,
        String alterationNumber,
        String description,
        String status,
        Long orderId,
        LocalDate createdAt
) {
}