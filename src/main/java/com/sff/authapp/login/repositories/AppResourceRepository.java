package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.AppResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppResourceRepository extends JpaRepository<AppResource, Long> {

    Optional<AppResource> findByResourceKey(String resourceKey);

    boolean existsByResourceKey(String resourceKey);

    List<AppResource> findAllByOrderBySortOrderAsc();
}
