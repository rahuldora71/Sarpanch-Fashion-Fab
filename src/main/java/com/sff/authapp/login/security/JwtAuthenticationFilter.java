package com.sff.authapp.login.security;

import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.helpers.UserHelper;
import com.sff.authapp.login.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header =
                request.getHeader("Authorization");

        if (header == null ||
                !header.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {

            // Must be an access token
            if (!jwtService.isAccessToken(token)) {

                request.setAttribute(
                        "error",
                        "Invalid access token"
                );

                filterChain.doFilter(request, response);
                return;
            }

            Jws<Claims> parsed =
                    jwtService.parse(token);

            Claims claims =
                    parsed.getPayload();

            String subject =
                    claims.getSubject();

            Long userId =
                    UserHelper.parseId(subject);

            userRepository
                    .findById(userId)
                    .ifPresent(user -> {

                        if (!user.isEnabled()) {
                            return;
                        }

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        user.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        if (SecurityContextHolder
                                .getContext()
                                .getAuthentication() == null) {

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(
                                            authentication
                                    );
                        }
                    });

        } catch (JwtException e) {

            request.setAttribute(
                    "error",
                    "Invalid or expired token"
            );

        } catch (Exception e) {

            request.setAttribute(
                    "error",
                    "Invalid authentication token"
            );
        }

        filterChain.doFilter(request, response);
    }


    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path =
                request.getRequestURI();

        return path.startsWith("/api/v1/auth")
                || path.startsWith("/oauth2")
                || path.startsWith("/login/oauth2")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }
}