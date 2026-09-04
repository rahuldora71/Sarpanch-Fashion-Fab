package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    Optional<RefreshToken> findByJtiAndRevokedFalse(String jti);

    List<RefreshToken> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}