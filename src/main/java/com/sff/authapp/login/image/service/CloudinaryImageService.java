package com.sff.authapp.login.image.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import com.sff.authapp.login.image.dto.ImageMetadata;
import com.sff.authapp.login.image.dto.ImageUploadResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryImageService
        implements ImageService {

    private final Cloudinary cloudinary;


    // ============================================================
    // UPLOAD IMAGE
    // ============================================================

    @Override
    public ImageUploadResponse uploadImage(
            MultipartFile file
    ) {

        validateFile(file);

        try {

            String publicId =
                    "sff/images/"
                            + UUID.randomUUID();

            Map<?, ?> result =
                    cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "public_id", publicId,
                                    "resource_type", "image"
                            )
                    );

            String secureUrl =
                    (String) result.get("secure_url");

            String url =
                    (String) result.get("url");

            String returnedPublicId =
                    (String) result.get("public_id");

            String resourceType =
                    (String) result.get("resource_type");

            String format =
                    (String) result.get("format");

            Integer width =
                    result.get("width") != null
                            ? ((Number) result.get("width")).intValue()
                            : null;

            Integer height =
                    result.get("height") != null
                            ? ((Number) result.get("height")).intValue()
                            : null;

            Long bytes =
                    result.get("bytes") != null
                            ? ((Number) result.get("bytes")).longValue()
                            : null;


            ImageMetadata metadata =
                    new ImageMetadata(
                            returnedPublicId,
                            url,
                            secureUrl,
                            resourceType,
                            format,
                            width,
                            height,
                            bytes
                    );


            return new ImageUploadResponse(
                    true,
                    "Image uploaded successfully",
                    secureUrl,
                    returnedPublicId,
                    metadata
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to upload image to Cloudinary",
                    e
            );
        }
    }


    // ============================================================
    // DELETE IMAGE
    // ============================================================

    @Override
    public void deleteImage(
            String publicId
    ) {

        if (publicId == null
                || publicId.isBlank()) {

            throw new IllegalArgumentException(
                    "Cloudinary public ID is required"
            );
        }

        try {

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image"
                    )
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to delete image from Cloudinary",
                    e
            );
        }
    }


    // ============================================================
    // VALIDATION
    // ============================================================

    private void validateFile(
            MultipartFile file
    ) {

        if (file == null
                || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Image file is required"
            );
        }


        String contentType =
                file.getContentType();

        if (contentType == null
                || !contentType.startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Only image files are allowed"
            );
        }
    }
}