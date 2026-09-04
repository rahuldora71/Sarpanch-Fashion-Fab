package com.sff.authapp.login.dtos.fabric;

import com.sff.authapp.login.entities.FabricSample;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FabricSampleCreateRequest(

        @NotBlank(message = "Book number is required")
        @Size(
                max = 20,
                message = "Book number cannot exceed 20 characters"
        )
        String bookNumber,

        @NotBlank(message = "Page number is required")
        @Size(
                max = 20,
                message = "Page number cannot exceed 20 characters"
        )
        String pageNumber,

        @Size(
                max = 100,
                message = "Color cannot exceed 100 characters"
        )
        String color,

        @Size(
                max = 255,
                message = "Description cannot exceed 255 characters"
        )
        String description,

        FabricSample.Status status
) {
}