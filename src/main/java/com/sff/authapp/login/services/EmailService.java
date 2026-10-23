package com.sff.authapp.login.services;

public interface EmailService {

    /**
     * Sends password reset OTP email with HTML and plain text fallback.
     *
     * @param toEmail   Recipient email address
     * @param otpCode   6-digit numeric OTP
     * @param ttlMinutes Expiration duration in minutes (e.g. 5)
     * @return true if sent or logged successfully, false otherwise
     */
    boolean sendPasswordResetOtp(String toEmail, String otpCode, int ttlMinutes);
}
