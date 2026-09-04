package com.sff.authapp.login.image.controller;

import com.sff.authapp.login.image.dto.ImageUploadResponse;
import com.sff.authapp.login.image.service.ImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(
        name = "Image Management",
        description = "APIs for uploading and deleting images using Cloudinary"
)
public class ImageController {


    private final ImageService imageService;


    // ============================================================
    // UPLOAD IMAGE
    // ============================================================

    @Operation(
            summary = "Upload an image",
            description = """
                    Uploads an image to Cloudinary and returns the
                    Cloudinary image URL and public ID.

                    The frontend should send the image using
                    multipart/form-data.

                    The returned imageUrl can then be stored in
                    entities such as TailoringService, AddOn,
                    Product, Worker, or Customer.

                    Supported users:
                    OWNER, ADMIN, WORKER
                    """
    )
    @ApiResponses({

            @ApiResponse(
                    responseCode = "201",
                    description = "Image uploaded successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation =
                                            ImageUploadResponse.class
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Invalid request. This can occur when:
                            - No image file is provided
                            - The uploaded file is empty
                            - The uploaded file is not an image
                            """
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "User is not authenticated"
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to upload images"
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to upload the image to Cloudinary"
            )
    })
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<ImageUploadResponse> uploadImage(

            @Parameter(
                    description = "Image file to upload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                    )
            )
            @RequestParam("file")
            MultipartFile file

    ) {

        ImageUploadResponse response =
                imageService.uploadImage(file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // DELETE IMAGE
    // ============================================================

    @Operation(
            summary = "Delete an image",
            description = """
                    Deletes an image from Cloudinary using its
                    Cloudinary public ID.

                    The publicId is returned when the image is
                    successfully uploaded.

                    Supported users:
                    OWNER, ADMIN
                    """
    )
    @ApiResponses({

            @ApiResponse(
                    responseCode = "204",
                    description = "Image deleted successfully"
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Cloudinary public ID is missing or invalid"
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "User is not authenticated"
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to delete images"
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to delete the image from Cloudinary"
            )
    })
    @DeleteMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN')"
    )
    public ResponseEntity<Void> deleteImage(

            @Parameter(
                    description = """
                            Cloudinary public ID of the image.

                            This value is returned as publicId
                            in the image upload response.

                            Example:
                            sff/images/550e8400-e29b-41d4-a716-446655440000
                            """,
                    required = true
            )
            @RequestParam("publicId")
            String publicId

    ) {

        imageService.deleteImage(publicId);

        return ResponseEntity
                .noContent()
                .build();
    }
}