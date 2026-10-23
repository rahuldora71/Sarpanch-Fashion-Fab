package com.sff.authapp.login.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sff.authapp.login.dtos.ApiError;
import com.sff.authapp.login.security.AuthRateLimitingFilter;
import com.sff.authapp.login.security.JwtAuthenticationFilter;
import com.sff.authapp.login.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitingFilter authRateLimitingFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final ObjectMapper objectMapper;


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // JWT based application
                .csrf(AbstractHttpConfigurer::disable)

                // CORS
                .cors(Customizer.withDefaults())

                // No server-side session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Authentication + OAuth2 + Swagger
                        .requestMatchers(
                                AppConstants.AUTH_PUBLIC_URLS
                        ).permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 ->
                        oauth2.successHandler(oAuth2SuccessHandler)
                )

                // We are handling JWT authentication manually
                .logout(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        (request, response, ex) -> {

                                                response.setStatus(
                                                        HttpStatus.UNAUTHORIZED.value()
                                                );

                                                response.setContentType(
                                                        "application/json"
                                                );

                                                String message =
                                                        "Unauthorized access";

                                                String error =
                                                        (String) request.getAttribute(
                                                                "error"
                                                        );

                                                if (error != null) {
                                                    message = error;
                                                }

                                                ApiError apiError =
                                                        ApiError.of(
                                                                HttpStatus.UNAUTHORIZED.value(),
                                                                "Unauthorized",
                                                                message,
                                                                request.getRequestURI(),
                                                                false
                                                        );

                                                response.getWriter().write(
                                                        objectMapper.writeValueAsString(
                                                                apiError
                                                        )
                                                );
                                        }
                                )
                                .accessDeniedHandler(
                                        (request, response, ex) -> {

                                                response.setStatus(
                                                        HttpStatus.FORBIDDEN.value()
                                                );

                                                response.setContentType(
                                                        "application/json"
                                                );

                                                ApiError apiError =
                                                        ApiError.of(
                                                                HttpStatus.FORBIDDEN.value(),
                                                                "Forbidden",
                                                                "Access Denied: You do not have sufficient permissions to perform this action.",
                                                                request.getRequestURI(),
                                                                false
                                                        );

                                                response.getWriter().write(
                                                        objectMapper.writeValueAsString(
                                                                apiError
                                                        )
                                                );
                                        }
                                )
                )

                // Security headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .xssProtection(Customizer.withDefaults())
                )

                // Rate limiting filter before JWT
                .addFilterBefore(
                        authRateLimitingFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource(

            @Value(
                    "${app.cors.frontend.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}"
            )
            String corsUrls

    ) {

        List<String> origins =
                Arrays.stream(corsUrls.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList();

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Allow localhost and any local network device on same Wi-Fi with credentials
        configuration.setAllowedOriginPatterns(List.of("*"));

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );

        // Required for refresh-token cookies
        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
