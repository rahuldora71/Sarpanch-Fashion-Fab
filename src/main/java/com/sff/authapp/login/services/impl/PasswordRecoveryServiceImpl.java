package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.passwordreset.PasswordResetResponse;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.RefreshTokenRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.security.PasswordResetChallenge;
import com.sff.authapp.login.services.EmailService;
import com.sff.authapp.login.services.PasswordRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.password-reset.otp-ttl-seconds:300}")
    private long otpTtlSeconds;

    @Value("${app.security.password-reset.resend-interval-seconds:60}")
    private long resendIntervalSeconds;

    @Value("${app.security.password-reset.max-attempts:5}")
    private int maxAttempts;

    private final Map<String, PasswordResetChallenge> challengeStore = new ConcurrentHashMap<>();
    private final Map<String, PasswordResetChallenge> tokenStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    // ============================================================
    // 1. INITIATE FORGOT PASSWORD
    // ============================================================

    @Override
    public PasswordResetResponse initiateForgotPassword(String rawEmail, String clientIp) {
        cleanExpiredChallenges();

        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address is required");
        }

        String email = rawEmail.trim().toLowerCase();
        log.info("Password reset request initiated for email pattern: {} from IP: {}", maskEmail(email), clientIp);

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            // Return generic challenge and message to prevent email enumeration
            String dummyChallengeId = UUID.randomUUID().toString();
            return PasswordResetResponse.initiated(
                    dummyChallengeId,
                    resendIntervalSeconds,
                    "If an account exists for this email, a verification code has been sent."
            );
        }

        User user = optionalUser.get();

        // Invalidate any existing challenges for this email
        invalidateChallengesForEmail(email);

        String otp = generateNumericOtp(6);
        String challengeId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .challengeId(challengeId)
                .email(user.getEmail())
                .otp(otp)
                .createdAt(now)
                .expiresAt(now.plusSeconds(otpTtlSeconds))
                .lastSentAt(now)
                .failedAttempts(0)
                .verified(false)
                .consumed(false)
                .build();

        challengeStore.put(challengeId, challenge);

        // Dispatch Email
        emailService.sendPasswordResetOtp(user.getEmail(), otp, (int) (otpTtlSeconds / 60));

        return PasswordResetResponse.initiated(
                challengeId,
                resendIntervalSeconds,
                "If an account exists for this email, a verification code has been sent."
        );
    }

    // ============================================================
    // 2. RESEND OTP
    // ============================================================

    @Override
    public PasswordResetResponse resendOtp(String challengeId, String clientIp) {
        cleanExpiredChallenges();

        PasswordResetChallenge challenge = challengeStore.get(challengeId);

        if (challenge == null || challenge.isExpired() || challenge.isVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reset session has expired or is invalid. Please request a new verification code."
            );
        }

        // Check 60s cooldown
        Instant cooldownEnd = challenge.getLastSentAt().plusSeconds(resendIntervalSeconds);
        Instant now = Instant.now();

        if (now.isBefore(cooldownEnd)) {
            long remaining = Duration.between(now, cooldownEnd).getSeconds() + 1;
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait " + remaining + " seconds before requesting another code."
            );
        }

        // Generate fresh OTP and reset attempt counter
        String freshOtp = generateNumericOtp(6);
        challenge.setOtp(freshOtp);
        challenge.setLastSentAt(now);
        challenge.setExpiresAt(now.plusSeconds(otpTtlSeconds));
        challenge.setFailedAttempts(0);

        log.info("Resending password reset OTP for challengeId: {} to email: {}", challengeId, maskEmail(challenge.getEmail()));

        emailService.sendPasswordResetOtp(challenge.getEmail(), freshOtp, (int) (otpTtlSeconds / 60));

        return PasswordResetResponse.initiated(
                challengeId,
                resendIntervalSeconds,
                "A fresh verification code has been sent to your email."
        );
    }

    // ============================================================
    // 3. VERIFY OTP
    // ============================================================

    @Override
    public PasswordResetResponse verifyOtp(String challengeId, String submittedOtp, String clientIp) {
        cleanExpiredChallenges();

        PasswordResetChallenge challenge = challengeStore.get(challengeId);

        if (challenge == null || challenge.isExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verification code has expired. Please request a new code."
            );
        }

        if (challenge.isVerified()) {
            return PasswordResetResponse.verified(
                    challenge.getResetToken(),
                    "Verification code already confirmed."
            );
        }

        if (challenge.getFailedAttempts() >= maxAttempts) {
            challengeStore.remove(challengeId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum attempt limit reached. Please request a new verification code."
            );
        }

        // Constant-time comparison to prevent timing attacks
        boolean matches = MessageDigest.isEqual(
                challenge.getOtp().getBytes(StandardCharsets.UTF_8),
                submittedOtp.trim().getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            int remainingAttempts = maxAttempts - challenge.getFailedAttempts();

            if (remainingAttempts <= 0) {
                challengeStore.remove(challengeId);
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Incorrect code. Maximum attempts exceeded. Please request a new code."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid verification code. " + remainingAttempts + " attempts remaining."
            );
        }

        // OTP Verified successfully -> issue single-use reset authorization token
        String resetToken = UUID.randomUUID().toString();
        Instant now = Instant.now();

        challenge.setVerified(true);
        challenge.setResetToken(resetToken);
        challenge.setResetTokenExpiresAt(now.plusSeconds(600)); // 10 minutes to complete password change

        tokenStore.put(resetToken, challenge);

        log.info("OTP verified successfully for challengeId: {}, reset token issued.", challengeId);

        return PasswordResetResponse.verified(
                resetToken,
                "Verification code confirmed successfully. Please enter your new password."
        );
    }

    // ============================================================
    // 4. RESET PASSWORD
    // ============================================================

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(String resetToken, String newPassword, String clientIp) {
        cleanExpiredChallenges();

        if (resetToken == null || resetToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset authorization token is required");
        }

        PasswordResetChallenge challenge = tokenStore.remove(resetToken);

        if (challenge == null || !challenge.isVerified() || challenge.isConsumed() || challenge.isResetTokenExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired password reset session. Please start the recovery process again."
            );
        }

        // Validate password strength
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters in length."
            );
        }

        User user = userRepository.findByEmail(challenge.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found."
                ));

        // Mark consumed and remove from challengeStore
        challenge.setConsumed(true);
        challengeStore.remove(challenge.getChallengeId());

        // Securely hash and update password
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        // Security safeguard: Revoke/delete all active refresh tokens for this user
        refreshTokenRepository.deleteAllByUserId(user.getId());

        log.info("Password successfully reset and sessions revoked for user: {}", maskEmail(user.getEmail()));

        return PasswordResetResponse.message(
                "Your password has been reset successfully. Please log in with your new password."
        );
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String generateNumericOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private void invalidateChallengesForEmail(String email) {
        challengeStore.values().removeIf(c -> email.equalsIgnoreCase(c.getEmail()));
    }

    private void cleanExpiredChallenges() {
        challengeStore.values().removeIf(PasswordResetChallenge::isExpired);
        tokenStore.values().removeIf(PasswordResetChallenge::isResetTokenExpired);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (prefix.length() <= 2) return prefix + "***" + domain;
        return prefix.charAt(0) + "***" + prefix.charAt(prefix.length() - 1) + domain;
    }
}
