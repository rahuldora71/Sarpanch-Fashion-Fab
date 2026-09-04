package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Field;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository
        extends JpaRepository<Field, Long> {

    boolean existsByNameIgnoreCase(
            String name
    );

    Page<Field> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

}