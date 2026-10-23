package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole(Role role);

    List<RolePermission> findByRoleId(Long roleId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    @Query("SELECT rp FROM RolePermission rp JOIN FETCH rp.permission p JOIN FETCH p.resource res WHERE rp.role.id = :roleId")
    List<RolePermission> findByRoleIdWithPermissionAndResource(@Param("roleId") Long roleId);

    @Query("SELECT rp FROM RolePermission rp JOIN FETCH rp.permission p JOIN FETCH p.resource res WHERE rp.role.id IN :roleIds AND rp.granted = true")
    List<RolePermission> findGrantedByRoleIdsWithPermissionAndResource(@Param("roleIds") List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
