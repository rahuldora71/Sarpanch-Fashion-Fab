package com.sff.authapp.login.dtos.passwordreset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String resetToken,

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String newPassword
) {
}
