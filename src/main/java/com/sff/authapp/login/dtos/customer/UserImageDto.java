package com.sff.authapp.login.dtos.customer;

import java.time.Instant;

public record UserImageDto(
        Long id,
        String imageUrl,
        String publicId,
        String purpose,
        Instant createdAt
) {
}
