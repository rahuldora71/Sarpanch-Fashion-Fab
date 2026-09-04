package com.sff.authapp.login.dtos.field;

import jakarta.validation.constraints.Size;

public record FieldUpdateRequestDto(

        @Size(
                max = 100,
                message = "Field name cannot exceed 100 characters"
        )
        String name

) {
}