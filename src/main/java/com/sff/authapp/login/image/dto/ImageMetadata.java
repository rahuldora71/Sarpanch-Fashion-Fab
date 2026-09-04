package com.sff.authapp.login.image.dto;

public record ImageMetadata(

        String publicId,

        String url,

        String secureUrl,

        String resourceType,

        String format,

        Integer width,

        Integer height,

        Long bytes
) {
}