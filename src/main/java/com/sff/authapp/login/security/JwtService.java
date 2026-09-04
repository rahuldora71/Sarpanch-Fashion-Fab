package com.sff.authapp.login.security;

import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Getter
public class JwtService {

    private final SecretKey key;

    private final long accessTtlSeconds;

    private final long refreshTtlSeconds;

    private final String issuer;


    public JwtService(

            @Value("${security.jwt.secret-key}")
            String secretKey,

            @Value("${security.jwt.access-ttl-seconds}")
            long accessTtlSeconds,

            @Value("${security.jwt.refresh-ttl-seconds}")
            long refreshTtlSeconds,

            @Value("${security.jwt.issuer}")
            String issuer

    ) {

        if (secretKey == null ||
                secretKey.length() < 64) {

            throw new IllegalArgumentException(
                    "Invalid JWT secret key"
            );
        }

        this.key =
                Keys.hmacShaKeyFor(
                        secretKey.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        this.accessTtlSeconds =
                accessTtlSeconds;

        this.refreshTtlSeconds =
                refreshTtlSeconds;

        this.issuer = issuer;
    }


    public String generateAccessToken(
            User user
    ) {

        Instant now = Instant.now();

        List<String> roles =
                user.getRoles() == null
                        ? List.of()
                        : user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .toList();

        return Jwts.builder()

                .id(UUID.randomUUID().toString())

                .subject(
                        user.getId().toString()
                )

                .issuer(issuer)

                .issuedAt(
                        java.util.Date.from(now)
                )

                .expiration(
                        java.util.Date.from(
                                now.plusSeconds(
                                        accessTtlSeconds
                                )
                        )
                )

                .claims(
                        Map.of(
                                "email",
                                user.getEmail(),

                                "name",
                                user.getName(),

                                "roles",
                                roles,

                                "typ",
                                "access"
                        )
                )

                .signWith(key)

                .compact();
    }


    public String generateRefreshToken(
            User user,
            String jti
    ) {

        Instant now = Instant.now();

        return Jwts.builder()

                .id(jti)

                .subject(
                        user.getId().toString()
                )

                .issuer(issuer)

                .issuedAt(
                        java.util.Date.from(now)
                )

                .expiration(
                        java.util.Date.from(
                                now.plusSeconds(
                                        refreshTtlSeconds
                                )
                        )
                )

                .claims(
                        Map.of(
                                "typ",
                                "refresh"
                        )
                )

                .signWith(key)

                .compact();
    }


    public Jws<Claims> parse(
            String token
    ) {

        return Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }


    public boolean isAccessToken(
            String token
    ) {

        Claims claims =
                parse(token).getPayload();

        return "access".equals(
                claims.get("typ")
        );
    }


    public boolean isRefreshToken(
            String token
    ) {

        Claims claims =
                parse(token).getPayload();

        return "refresh".equals(
                claims.get("typ")
        );
    }


    public Long getUserId(
            String token
    ) {

        return Long.parseLong(
                parse(token)
                        .getPayload()
                        .getSubject()
        );
    }


    public String getJti(
            String token
    ) {

        return parse(token)
                .getPayload()
                .getId();
    }


    @SuppressWarnings("unchecked")
    public List<String> getRoles(
            String token
    ) {

        Object roles =
                parse(token)
                        .getPayload()
                        .get("roles");

        if (roles instanceof List<?> list) {

            return list.stream()
                    .map(Object::toString)
                    .toList();
        }

        return List.of();
    }


    public String getEmail(
            String token
    ) {

        return (String) parse(token)
                .getPayload()
                .get("email");
    }
}