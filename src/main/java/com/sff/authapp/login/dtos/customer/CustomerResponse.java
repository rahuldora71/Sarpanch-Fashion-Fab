package com.sff.authapp.login.dtos.customer;

import java.time.Instant;

public record CustomerResponse(

        Long id,

        String customerCode,

        String name,

        String phone,

        String alternatePhone,

        String email,

        String image,

        String address,

        String city,

        String notes,

        boolean enable,

        Instant createdAt,

        Instant updatedAt
) {
}