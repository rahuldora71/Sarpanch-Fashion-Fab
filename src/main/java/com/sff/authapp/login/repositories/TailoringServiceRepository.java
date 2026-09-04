package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.TailoringService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TailoringServiceRepository
        extends JpaRepository<TailoringService, Long> {

    // ============================================================
    // NAME
    // ============================================================

    boolean existsByNameIgnoreCase(String name);

    Optional<TailoringService> findByNameIgnoreCase(String name);


    // ============================================================
    // ACTIVE / INACTIVE
    // ============================================================

    Page<TailoringService> findByActive(
            boolean active,
            Pageable pageable
    );

    Page<TailoringService> findByActiveTrue(
            Pageable pageable
    );

    Page<TailoringService> findByActiveFalse(
            Pageable pageable
    );


    // ============================================================
    // CATEGORY
    // ============================================================

    Page<TailoringService> findByCategoryIgnoreCase(
            String category,
            Pageable pageable
    );


    // ============================================================
    // SEARCH BY NAME
    // ============================================================

    Page<TailoringService> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );


    // ============================================================
    // SEARCH BY NAME OR CATEGORY
    // ============================================================

    Page<TailoringService>
    findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String name,
            String category,
            Pageable pageable
    );


    // ============================================================
    // SEARCH ACTIVE / INACTIVE SERVICES
    // ============================================================

    @Query("""
        SELECT s
        FROM TailoringService s
        WHERE s.active = :active
        AND (
            LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR
            LOWER(s.category) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """)
    Page<TailoringService> searchActiveServices(
            @Param("active") boolean active,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}