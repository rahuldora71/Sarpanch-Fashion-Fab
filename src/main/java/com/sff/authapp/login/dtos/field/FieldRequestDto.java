package com.sff.authapp.login.dtos.field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FieldRequestDto(

        @NotBlank(message = "Field name is required")
        @Size(
                max = 100,
                message = "Field name cannot exceed 100 characters"
        )
        String name

) {
}