package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByPermissionKey(String permissionKey);

    boolean existsByPermissionKey(String permissionKey);

    List<Permission> findByResourceResourceKey(String resourceKey);

    List<Permission> findByResourceId(Long resourceId);
}
