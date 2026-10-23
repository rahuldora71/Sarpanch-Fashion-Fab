package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserImageRepository extends JpaRepository<UserImage, Long> {

    List<UserImage> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserImage> findByUserIdOrderByIdDesc(Long userId);

    Optional<UserImage> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
