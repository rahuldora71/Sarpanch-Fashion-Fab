package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.dtos.rbac.*;

import java.util.List;

public interface DynamicPermissionService {

    List<AppResourceDto> getAllResources();

    List<RoleDto> getAllRoles();

    RolePermissionMatrixDto getRolePermissionsMatrix(Long roleId);

    void updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request, String actorEmail, String ipAddress);

    List<UserAccessDto> getAllUsersWithAccess();

    void assignUserRoles(Long userId, UserRoleAssignmentRequest request, String actorEmail, String ipAddress);

    List<AuditAccessLogDto> getAuditLogs();
}
