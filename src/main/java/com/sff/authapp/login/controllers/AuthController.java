package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.*;
import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.entities.RefreshToken;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.RefreshTokenRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.security.CookieService;
import com.sff.authapp.login.security.JwtService;
import com.sff.authapp.login.services.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;


    // ============================================================
    // LOGIN
    // ============================================================

    /**
     * Authenticate user and issue access + refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {

        // 1. Authenticate user
        Authentication authentication = authenticate(loginRequest);

        // 2. Find user
        User user = userRepository
                .findByEmail(loginRequest.email())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid Username or Password")
                );

        // 3. Check whether account is enabled
        if (!user.isEnable()) {
            throw new DisabledException("User is disabled");
        }

        // 4. Create refresh-token database record
        String jti = UUID.randomUUID().toString();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(
                        Instant.now()
                                .plusSeconds(jwtService.getRefreshTtlSeconds())
                )
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        // 5. Generate access token
        String accessToken = jwtService.generateAccessToken(user);

        // 6. Generate refresh token
        String refreshToken = jwtService.generateRefreshToken(
                user,
                refreshTokenEntity.getJti()
        );

        // 7. Store refresh token in HttpOnly cookie
        cookieService.attachRefreshTokenCookie(
                response,
                refreshToken,
                (int) jwtService.getRefreshTtlSeconds()
        );

        // 8. Prevent caching of authentication response
        cookieService.addNoStoreHeader(response);

        // 9. Convert User -> LoginResponse
        LoginResponse loginResponse = toLoginResponse(user);

        // 10. Build final response
        TokenResponse tokenResponse = TokenResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTtlSeconds(),
                loginResponse
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(tokenResponse);
    }


    // ============================================================
    // AUTHENTICATION
    // ============================================================

    private Authentication authenticate(LoginRequest loginRequest) {

        try {

            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

        } catch (Exception e) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }
    }


    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    /**
     * Rotate an existing refresh token and issue
     * a new access + refresh token pair.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws BadRequestException {

        // 1. Read refresh token
        String refreshToken = readRefreshTokenFromRequest(
                body,
                request
        ).orElseThrow(() ->
                new BadRequestException("Refresh token is required")
        );

        // 2. Validate token type
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException(
                    "Invalid refresh token"
            );
        }

        // 3. Extract token information
        String jti = jwtService.getJti(refreshToken);
        Long userId = jwtService.getUserId(refreshToken);

        // 4. Find refresh token in database
        RefreshToken storedRefreshToken =
                refreshTokenRepository
                        .findByJti(jti)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Refresh token not found in database"
                                )
                        );

        // 5. Check revoked
        if (storedRefreshToken.isRevoked()) {
            throw new BadRequestException(
                    "Refresh token is revoked"
            );
        }

        // 6. Check expiration
        if (storedRefreshToken
                .getExpiresAt()
                .isBefore(Instant.now())) {

            throw new BadRequestException(
                    "Refresh token is expired"
            );
        }

        // 7. Make sure token belongs to user
        if (!storedRefreshToken
                .getUser()
                .getId()
                .equals(userId)) {

            throw new BadRequestException(
                    "Refresh token does not belong to the user"
            );
        }

        // ========================================================
        // ROTATE REFRESH TOKEN
        // ========================================================

        // 8. Revoke old refresh token
        storedRefreshToken.setRevoked(true);

        String newJti = UUID.randomUUID().toString();

        storedRefreshToken.setReplacedByToken(newJti);

        refreshTokenRepository.save(storedRefreshToken);

        // 9. Get user
        User user = storedRefreshToken.getUser();

        // 10. Create new refresh token database record
        RefreshToken newRefreshTokenEntity =
                RefreshToken.builder()
                        .jti(newJti)
                        .user(user)
                        .createdAt(Instant.now())
                        .expiresAt(
                                Instant.now()
                                        .plusSeconds(
                                                jwtService
                                                        .getRefreshTtlSeconds()
                                        )
                        )
                        .revoked(false)
                        .build();

        refreshTokenRepository.save(newRefreshTokenEntity);

        // 11. Generate new access token
        String newAccessToken =
                jwtService.generateAccessToken(user);

        // 12. Generate new refresh token
        String newRefreshToken =
                jwtService.generateRefreshToken(
                        user,
                        newRefreshTokenEntity.getJti()
                );

        // 13. Update refresh-token cookie
        cookieService.attachRefreshTokenCookie(
                response,
                newRefreshToken,
                (int) jwtService.getRefreshTtlSeconds()
        );

        // 14. Prevent caching
        cookieService.addNoStoreHeader(response);

        // 15. Convert User -> LoginResponse
        LoginResponse loginResponse = toLoginResponse(user);

        // 16. Build response
        TokenResponse tokenResponse = TokenResponse.of(
                newAccessToken,
                null,
                jwtService.getAccessTtlSeconds(),
                loginResponse
        );

        return ResponseEntity.ok(tokenResponse);
    }


    // ============================================================
    // LOGOUT
    // ============================================================

    /**
     * Revoke current refresh token and clear authentication cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletResponse response,
            HttpServletRequest request
    ) {

        readRefreshTokenFromRequest(
                null,
                request
        ).ifPresent(token -> {

            try {

                if (jwtService.isRefreshToken(token)) {

                    String jti =
                            jwtService.getJti(token);

                    refreshTokenRepository
                            .findByJti(jti)
                            .ifPresent(refreshToken -> {

                                refreshToken.setRevoked(true);

                                refreshTokenRepository.save(
                                        refreshToken
                                );
                            });
                }

            } catch (JwtException ignored) {
                // Invalid token.
                // Logout should still clear the cookie.
            }
        });

        // Clear refresh-token cookie
        cookieService.clearRefreshTokenCookie(response);

        // Prevent caching
        cookieService.addNoStoreHeader(response);

        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }


    // ============================================================
    // READ REFRESH TOKEN
    // ============================================================

    /**
     * Read refresh token from:
     *
     * 1. Cookie
     * 2. Request body
     * 3. X-Refresh-Token header
     * 4. Authorization Bearer header
     */
    private Optional<String> readRefreshTokenFromRequest(
            RefreshTokenRequest body,
            HttpServletRequest request
    ) {

        // ========================================================
        // 1. COOKIE
        // ========================================================

        if (request.getCookies() != null) {

            Optional<String> fromCookie =
                    Arrays.stream(request.getCookies())
                            .filter(cookie ->
                                    cookieService
                                            .getRefreshTokenCookieName()
                                            .equals(cookie.getName())
                            )
                            .map(Cookie::getValue)
                            .filter(value ->
                                    !value.isBlank()
                            )
                            .findFirst();

            if (fromCookie.isPresent()) {
                return fromCookie;
            }
        }


        // ========================================================
        // 2. REQUEST BODY
        // ========================================================

        if (body != null
                && body.RefreshToken() != null
                && !body.RefreshToken().isBlank()) {

            return Optional.of(
                    body.RefreshToken()
            );
        }


        // ========================================================
        // 3. CUSTOM HEADER
        // ========================================================

        String headerRefreshToken =
                request.getHeader("X-Refresh-Token");

        if (headerRefreshToken != null
                && !headerRefreshToken.isBlank()) {

            return Optional.of(
                    headerRefreshToken
            );
        }


        // ========================================================
        // 4. AUTHORIZATION HEADER
        // ========================================================

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader != null
                && authHeader.regionMatches(
                true,
                0,
                "Bearer ",
                0,
                7
        )) {

            String candidate =
                    authHeader.substring(7);

            if (!candidate.isEmpty()) {

                try {

                    if (jwtService.isRefreshToken(candidate)) {
                        return Optional.of(candidate);
                    }

                } catch (Exception ignored) {
                    // Invalid token.
                }
            }
        }

        return Optional.empty();
    }


    // ============================================================
    // REGISTER
    // ============================================================

    /**
     * Create a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> registerUser(
            @RequestBody UserDto userDto
    ) {

        LoginResponse createdUser =
                authService.registerUser(userDto);

        LoginResponse response =
                new LoginResponse(
                        createdUser.id(),
                        createdUser.email(),
                        createdUser.name(),
                        createdUser.image(),
                        createdUser.enable(),
                        createdUser.createdAt(),
                        createdUser.updatedAt(),
                        createdUser.provider(),
                        createdUser.roles()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // USER -> LOGIN RESPONSE
    // ============================================================

    /**
     * Convert User entity into a safe LoginResponse.
     *
     * We don't use ModelMapper here because LoginResponse
     * is a Java record and ModelMapper requires a no-args
     * constructor by default.
     */
    private LoginResponse toLoginResponse(User user) {

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getImage(),
                user.isEnable(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getProvider(),
                user.getRoles()
                        .stream()
                        .map(role ->
                                new RoleDto(
                                        role.getId(),
                                        role.getName()
                                )
                        )
                        .collect(Collectors.toSet())
        );
    }
}