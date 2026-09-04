package com.sff.authapp.login.config;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String[] AUTH_PUBLIC_URLS = {

            // Authentication
            "/api/v1/auth/**",
            "/api/v1/roles/**",
            // OAuth2
            "/oauth2/**",
            "/login/oauth2/**",

            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };
    public static final String WORKER_ROLE = "ROLE_WORKER";
}