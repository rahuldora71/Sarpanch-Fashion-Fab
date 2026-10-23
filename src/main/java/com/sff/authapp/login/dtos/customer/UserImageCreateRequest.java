package com.sff.authapp.login.dtos.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserImageCreateRequest(
        @NotBlank(message = "Image URL is required")
        @Size(max = 1000, message = "Image URL cannot exceed 1000 characters")
        String imageUrl,

        @Size(max = 255, message = "Public ID cannot exceed 255 characters")
        String publicId,

        @Size(max = 255, message = "Purpose cannot exceed 255 characters")
        String purpose
) {
}
