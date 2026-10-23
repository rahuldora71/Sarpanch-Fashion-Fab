package com.sff.authapp.login.security;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class PasswordResetChallenge {

    private String challengeId;
    private String email;
    private String otp;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastSentAt;
    private int failedAttempts;
    private boolean verified;
    private String resetToken;
    private Instant resetTokenExpiresAt;
    private boolean consumed;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isResetTokenExpired() {
        return resetTokenExpiresAt == null || Instant.now().isAfter(resetTokenExpiresAt);
    }
}
