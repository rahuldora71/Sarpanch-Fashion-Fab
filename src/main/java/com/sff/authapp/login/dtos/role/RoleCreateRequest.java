package com.sff.authapp.login.dtos.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(

        @NotBlank(message = "Role name is required")
        @Size(
                min = 3,
                max = 50,
                message = "Role name must be between 3 and 50 characters"
        )
        String name
) {
}