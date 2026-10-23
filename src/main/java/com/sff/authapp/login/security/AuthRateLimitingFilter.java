package com.sff.authapp.login.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sff.authapp.login.dtos.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window rate limiting filter for sensitive authentication endpoints.
 * Protects against brute-force attacks, automated credential stuffing, and email flooding.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    private static class RateWindow {
        Instant windowStart;
        AtomicInteger count;

        RateWindow(Instant start) {
            this.windowStart = start;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RateWindow> requestCounters = new ConcurrentHashMap<>();

    private static final int WINDOW_SECONDS = 60;
    private static final int MAX_LOGIN_PER_WINDOW = 15;
    private static final int MAX_PASSWORD_RECOVERY_PER_WINDOW = 6;
    private static final int MAX_OTP_VERIFY_PER_WINDOW = 15;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only enforce on auth POST requests
        if ("POST".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/v1/auth/")) {

            int maxAllowed = getRateLimitForPath(path);

            if (maxAllowed > 0) {
                String clientIp = extractClientIp(request);
                String key = clientIp + ":" + path;

                Instant now = Instant.now();
                RateWindow window = requestCounters.compute(key, (k, existing) -> {
                    if (existing == null || now.isAfter(existing.windowStart.plusSeconds(WINDOW_SECONDS))) {
                        return new RateWindow(now);
                    }
                    existing.count.incrementAndGet();
                    return existing;
                });

                if (window.count.get() > maxAllowed) {
                    log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, path);

                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));

                    ApiError apiError = ApiError.of(
                            HttpStatus.TOO_MANY_REQUESTS.value(),
                            "Too Many Requests",
                            "Rate limit exceeded. Please wait a minute before retrying.",
                            path,
                            false
                    );

                    response.getWriter().write(objectMapper.writeValueAsString(apiError));
                    return;
                }
            }
        }

        // Clean up stale counters if map grows
        if (requestCounters.size() > 2000) {
            Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS * 2);
            requestCounters.entrySet().removeIf(entry -> entry.getValue().windowStart.isBefore(cutoff));
        }

        filterChain.doFilter(request, response);
    }

    private int getRateLimitForPath(String path) {
        if (path.endsWith("/login")) {
            return MAX_LOGIN_PER_WINDOW;
        }
        if (path.endsWith("/forgot-password") || path.endsWith("/resend-reset-otp") || path.endsWith("/reset-password")) {
            return MAX_PASSWORD_RECOVERY_PER_WINDOW;
        }
        if (path.endsWith("/verify-reset-otp")) {
            return MAX_OTP_VERIFY_PER_WINDOW;
        }
        return -1;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
