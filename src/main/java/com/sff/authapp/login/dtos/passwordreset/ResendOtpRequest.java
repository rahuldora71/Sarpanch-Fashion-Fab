package com.sff.authapp.login.dtos.passwordreset;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "Challenge ID is required")
        String challengeId
) {
}
