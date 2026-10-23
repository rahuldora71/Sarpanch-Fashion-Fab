package com.sff.authapp.login.dtos.passwordreset;

public record PasswordResetResponse(
        boolean success,
        String message,
        String challengeId,
        String resetToken,
        long cooldownSeconds
) {
    public static PasswordResetResponse message(String message) {
        return new PasswordResetResponse(true, message, null, null, 0);
    }

    public static PasswordResetResponse initiated(String challengeId, long cooldownSeconds, String message) {
        return new PasswordResetResponse(true, message, challengeId, null, cooldownSeconds);
    }

    public static PasswordResetResponse verified(String resetToken, String message) {
        return new PasswordResetResponse(true, message, null, resetToken, 0);
    }
}
