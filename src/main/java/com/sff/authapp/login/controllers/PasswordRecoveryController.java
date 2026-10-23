package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.passwordreset.*;
import com.sff.authapp.login.services.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    /**
     * Step 1: Request OTP for password reset.
     * Always returns a generic response to prevent user enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PasswordResetResponse response = passwordRecoveryService.initiateForgotPassword(request.email(), clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2 (Optional): Resend OTP after 60-second cooldown expires.
     */
    @PostMapping("/resend-reset-otp")
    public ResponseEntity<PasswordResetResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PasswordResetResponse response = passwordRecoveryService.resendOtp(request.challengeId(), clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 3: Verify the 6-digit OTP code.
     * Returns a short-lived, single-use reset authorization token.
     */
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<PasswordResetResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PasswordResetResponse response = passwordRecoveryService.verifyOtp(request.challengeId(), request.otp(), clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 4: Finalize password change using the verified reset authorization token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PasswordResetResponse response = passwordRecoveryService.resetPassword(request.resetToken(), request.newPassword(), clientIp);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
