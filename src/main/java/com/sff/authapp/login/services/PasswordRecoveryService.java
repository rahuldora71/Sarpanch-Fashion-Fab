package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.passwordreset.PasswordResetResponse;

public interface PasswordRecoveryService {

    PasswordResetResponse initiateForgotPassword(String email, String clientIp);

    PasswordResetResponse resendOtp(String challengeId, String clientIp);

    PasswordResetResponse verifyOtp(String challengeId, String otp, String clientIp);

    PasswordResetResponse resetPassword(String resetToken, String newPassword, String clientIp);
}
