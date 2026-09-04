package com.sff.authapp.login.image.dto;

public record ImageUploadResponse(

        boolean success,

        String message,

        String imageUrl,

        String publicId,

        ImageMetadata metadata
) {
}