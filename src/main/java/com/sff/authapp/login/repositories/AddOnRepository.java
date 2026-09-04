package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.AddOn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddOnRepository
        extends JpaRepository<AddOn, Long> {

    // ============================================================
    // NAME
    // ============================================================

    boolean existsByNameIgnoreCase(String name);

    Optional<AddOn> findByNameIgnoreCase(String name);






    // ============================================================
    // ACTIVE / INACTIVE
    // ============================================================

    Page<AddOn> findByActiveTrue(
            Pageable pageable
    );

    Page<AddOn> findByActiveFalse(
            Pageable pageable
    );





    // ============================================================
    // SEARCH
    // ============================================================

    Page<AddOn> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );




    // ============================================================
    // SEARCH ACTIVE ADD-ONS
    // ============================================================

    Page<AddOn> findByActiveTrueAndNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
}