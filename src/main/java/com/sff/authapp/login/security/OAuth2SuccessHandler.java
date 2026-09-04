package com.sff.authapp.login.security;

import com.sff.authapp.login.entities.Provider;
import com.sff.authapp.login.entities.RefreshToken;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.RefreshTokenRepository;
import com.sff.authapp.login.repositories.RoleRepository;
import com.sff.authapp.login.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtService jwtService;
    private final CookieService cookieService;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontEndSuccessURL;

    @Value("${app.auth.frontend.failure-redirect}")
    private String frontEndFailureURL;


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        try {

            OAuth2User oauthUser =
                    (OAuth2User) authentication.getPrincipal();

            String registrationId =
                    getRegistrationId(authentication);

            if (!"google".equalsIgnoreCase(registrationId)) {
                throw new IllegalArgumentException(
                        "Unsupported  OAuth provider: " + registrationId
                );
            }

            User user = processGoogleUser(oauthUser);

            /*
             * Make sure the account is enabled.
             */
            if (!user.isEnabled()) {
                user.setEnable(true);
                user = userRepository.save(user);
            }

            /*
             * Create refresh-token database record.
             */
            String jti = UUID.randomUUID().toString();

            Instant now = Instant.now();

            RefreshToken refreshTokenEntity =
                    RefreshToken.builder()
                            .jti(jti)
                            .user(user)
                            .createdAt(now)
                            .expiresAt(
                                    now.plusSeconds(
                                            jwtService.getRefreshTtlSeconds()
                                    )
                            )
                            .revoked(false)
                            .build();

            refreshTokenRepository.save(refreshTokenEntity);

            /*
             * Generate refresh JWT.
             */
            String refreshToken =
                    jwtService.generateRefreshToken(
                            user,
                            jti
                    );

            /*
             * Store refresh token in HttpOnly cookie.
             */
            cookieService.attachRefreshTokenCookie(
                    response,
                    refreshToken,
                    jwtService.getRefreshTtlSeconds()
            );

            /*
             * Don't cache authentication response.
             */
            cookieService.addNoStoreHeader(response);

            /*
             * Do NOT put access token in URL.
             *
             * React will call /api/v1/auth/refresh
             * after the redirect.
             */
            response.sendRedirect(frontEndSuccessURL);

        } catch (Exception e) {

            log.error("Google OAuth authentication success handler failed", e);

            response.sendRedirect(frontEndFailureURL);
        }
    }


    private String getRegistrationId(
            Authentication authentication
    ) {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {

            return oauthToken
                    .getAuthorizedClientRegistrationId();
        }

        throw new IllegalArgumentException(
                "Invalid OAuth2 authentication"
        );
    }


    private User processGoogleUser(
            OAuth2User oauthUser
    ) {

        String googleId =
                getAttribute(oauthUser, "sub");

        String email =
                getAttribute(oauthUser, "email");

        String name =
                getAttribute(oauthUser, "name");

        String picture =
                getAttribute(oauthUser, "picture");


        if (email == null || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Google account does not provide an email"
            );
        }


        return userRepository
                .findByEmail(email)
                .map(existingUser -> {

                    /*
                     * Existing account.
                     */
                    existingUser.setProvider(
                            Provider.GOOGLE
                    );

                    existingUser.setProviderId(
                            googleId
                    );

                    if (name != null && !name.isBlank()) {
                        existingUser.setName(name);
                    }

                    if (picture != null && !picture.isBlank()) {
                        existingUser.setImage(picture);
                    }

                    existingUser.setEnable(true);

                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {

                    /*
                     * New Google account.
                     */
                    User newUser =
                            User.builder()
                                    .email(email)
                                    .name(
                                            name != null && !name.isBlank()
                                                    ? name
                                                    : email
                                    )
                                    .image(picture)
                                    .password(null)
                                    .enable(true)
                                    .provider(Provider.GOOGLE)
                                    .providerId(googleId)
                                    .build();

                    /*
                     * New OAuth users are customers
                     * by default.
                     */
                    addCustomerRole(newUser);

                    return userRepository.save(newUser);
                });
    }


    private void addCustomerRole(
            User user
    ) {

        Role customerRole =
                roleRepository
                        .findByName("ROLE_CUSTOMER")
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "ROLE_CUSTOMER not found"
                                )
                        );

        user.getRoles().add(customerRole);
    }


    private String getAttribute(
            OAuth2User user,
            String attribute
    ) {

        Object value =
                user.getAttributes().get(attribute);

        return value != null
                ? value.toString()
                : null;
    }
}