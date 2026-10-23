package com.sff.authapp.login.dtos.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerProfilePictureRequest(
        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        String imageUrl,

        @Size(max = 255, message = "Public ID cannot exceed 255 characters")
        String publicId
) {
}
