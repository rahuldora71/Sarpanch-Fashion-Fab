package com.sff.authapp.login.config;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String[] AUTH_PUBLIC_URLS = {

            // Authentication
            "/api/v1/auth/**",

            // Public Customer Order Tracking & Reviews (no auth required)
            "/api/v1/public/**",

            // Meta WhatsApp Cloud API Webhook (GET token verification & POST HMAC-SHA256 signature verification)
            "/api/whatsapp/webhook",

            // OAuth2
            "/oauth2/**",
            "/login/oauth2/**",

            // Actuator Health & Info (for Render / cloud deployment readiness probes)
            "/actuator/health",
            "/actuator/info",

            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };
    public static final String WORKER_ROLE = "ROLE_WORKER";
}
