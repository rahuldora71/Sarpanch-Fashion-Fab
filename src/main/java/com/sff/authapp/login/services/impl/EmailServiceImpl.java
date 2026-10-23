package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.services.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public boolean sendPasswordResetOtp(String toEmail, String otpCode, int ttlMinutes) {
        log.info("Initiating password reset email to recipient: {}", maskEmail(toEmail));

        // Always log OTP in dev mode for testing convenience
        if ("dev".equalsIgnoreCase(activeProfile) || "test".equalsIgnoreCase(activeProfile)) {
            log.info("[DEVELOPMENT MODE] Password Reset OTP for {}: {}", toEmail, otpCode);
        }

        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            log.warn("JavaMailSender is not configured or spring.mail.username is empty. " +
                     "Skipping SMTP dispatch. If in dev mode, use the OTP logged above.");
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, "Sarpanch Fashion Fab Security");
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Code — Sarpanch Fashion Fab");

            String htmlBody = buildHtmlTemplate(otpCode, ttlMinutes);
            String plainText = buildPlainTextTemplate(otpCode, ttlMinutes);

            helper.setText(plainText, htmlBody);

            mailSender.send(message);
            log.info("Successfully sent password reset email to: {}", maskEmail(toEmail));
            return true;

        } catch (Exception e) {
            log.error("Failed to send password reset email via SMTP to {}: {}", maskEmail(toEmail), e.getMessage());
            // In dev mode, return true so testing continues using logged OTP
            return "dev".equalsIgnoreCase(activeProfile);
        }
    }

    private String buildHtmlTemplate(String otp, int ttlMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Password Reset Request</title>
              <style>
                body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; color: #1e293b; }
                .wrapper { max-width: 580px; margin: 30px auto; background: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                .header { background: #064e3b; padding: 28px 32px; text-align: left; }
                .logo-text { color: #ffffff; font-size: 20px; font-weight: 800; letter-spacing: 0.05em; margin: 0; }
                .logo-sub { color: #6ee7b7; font-size: 11px; letter-spacing: 0.25em; margin: 2px 0 0 0; text-transform: uppercase; }
                .content { padding: 36px 32px; }
                h1 { font-size: 20px; font-weight: 700; color: #0f172a; margin-top: 0; margin-bottom: 12px; }
                p { font-size: 15px; line-height: 1.6; color: #475569; margin: 0 0 20px 0; }
                .otp-box { background: #f0fdf4; border: 2px dashed #059669; border-radius: 12px; padding: 24px; text-align: center; margin: 28px 0; }
                .otp-label { font-size: 12px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.1em; color: #047857; margin-bottom: 8px; }
                .otp-code { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 38px; font-weight: 800; letter-spacing: 8px; color: #065f46; margin: 0; }
                .otp-expire { font-size: 13px; color: #059669; font-weight: 600; margin-top: 8px; }
                .notice { background: #fffbeb; border-left: 4px solid #f59e0b; padding: 14px 16px; border-radius: 6px; font-size: 13px; color: #92400e; line-height: 1.5; margin-bottom: 24px; }
                .footer { background: #f8fafc; padding: 20px 32px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8; text-align: center; line-height: 1.5; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header">
                  <div class="logo-text">SARPANCH</div>
                  <div class="logo-sub">FASHION FAB TAILOR ERP</div>
                </div>
                <div class="content">
                  <h1>Password Reset Request</h1>
                  <p>Hello,</p>
                  <p>We received a request to reset the password for your <strong>Sarpanch Fashion Fab</strong> account.</p>
                  
                  <div class="otp-box">
                    <div class="otp-label">Your Verification Code</div>
                    <div class="otp-code">%s</div>
                    <div class="otp-expire">⏱️ Valid for %d minutes</div>
                  </div>

                  <div class="notice">
                    <strong>Security Notice:</strong> Never share this code with anyone, including Sarpanch Fashion Fab staff. We will never call or message you asking for this OTP.
                  </div>

                  <p>If you did not request a password reset, you can safely ignore this email. Your account remains secure and your password will not be changed.</p>
                  
                  <p style="margin-bottom:0;">Regards,<br><strong>Sarpanch Fashion Fab Security Team</strong></p>
                </div>
                <div class="footer">
                  This is an automated system notification from Sarpanch Fashion Fab Tailor ERP.<br>
                  Please do not reply directly to this email.
                </div>
              </div>
            </body>
            </html>
            """.formatted(otp, ttlMinutes);
    }

    private String buildPlainTextTemplate(String otp, int ttlMinutes) {
        return """
            Hello,

            We received a request to reset the password for your Sarpanch Fashion Fab account.

            Your verification code is:
            %s

            This code expires in %d minutes. For your security, never share this code with anyone.

            If you did not request a password reset, you can safely ignore this email. Your password will remain unchanged.

            Regards,
            Sarpanch Fashion Fab Security Team
            """.formatted(otp, ttlMinutes);
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
