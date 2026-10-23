package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.dtos.rbac.*;
import com.sff.authapp.login.dtos.role.RoleCreateRequest;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.security.PermissionSecurityService;
import com.sff.authapp.login.services.DynamicPermissionService;
import com.sff.authapp.login.services.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final DynamicPermissionService dynamicPermissionService;
    private final PermissionSecurityService permissionSecurityService;

    @GetMapping
    @PreAuthorize("@perm.can('roles_permissions', 'VIEW') or @perm.isAdmin()")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(dynamicPermissionService.getAllRoles());
    }

    @PostMapping
    @PreAuthorize("@perm.can('roles_permissions', 'CREATE') or @perm.isAdmin()")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleCreateRequest request) {
        RoleDto response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/resources")
    @PreAuthorize("@perm.can('roles_permissions', 'VIEW') or @perm.isAdmin()")
    public ResponseEntity<List<AppResourceDto>> getAllResources() {
        return ResponseEntity.ok(dynamicPermissionService.getAllResources());
    }

    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("@perm.can('roles_permissions', 'VIEW') or @perm.isAdmin()")
    public ResponseEntity<RolePermissionMatrixDto> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(dynamicPermissionService.getRolePermissionsMatrix(roleId));
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("@perm.can('roles_permissions', 'EDIT') or @perm.isAdmin()")
    public ResponseEntity<Map<String, String>> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody UpdateRolePermissionsRequest request,
            HttpServletRequest servletRequest
    ) {
        User currentUser = permissionSecurityService.getCurrentUser();
        String actor = currentUser != null ? currentUser.getEmail() : "ADMIN";
        String ip = servletRequest.getRemoteAddr();

        dynamicPermissionService.updateRolePermissions(roleId, request, actor, ip);
        return ResponseEntity.ok(Map.of("message", "Role permissions updated successfully"));
    }

    @GetMapping("/users")
    @PreAuthorize("@perm.can('roles_permissions', 'VIEW') or @perm.isAdmin()")
    public ResponseEntity<List<UserAccessDto>> getUsersWithAccess() {
        return ResponseEntity.ok(dynamicPermissionService.getAllUsersWithAccess());
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("@perm.can('roles_permissions', 'EDIT') or @perm.isAdmin()")
    public ResponseEntity<Map<String, String>> assignUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleAssignmentRequest request,
            HttpServletRequest servletRequest
    ) {
        User currentUser = permissionSecurityService.getCurrentUser();
        String actor = currentUser != null ? currentUser.getEmail() : "ADMIN";
        String ip = servletRequest.getRemoteAddr();

        dynamicPermissionService.assignUserRoles(userId, request, actor, ip);
        return ResponseEntity.ok(Map.of("message", "User roles updated successfully"));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("@perm.can('roles_permissions', 'VIEW') or @perm.isAdmin()")
    public ResponseEntity<List<AuditAccessLogDto>> getAuditLogs() {
        return ResponseEntity.ok(dynamicPermissionService.getAuditLogs());
    }
}
