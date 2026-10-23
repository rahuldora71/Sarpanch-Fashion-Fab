package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.dtos.rbac.*;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.security.PermissionSecurityService;
import com.sff.authapp.login.services.DynamicPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPermissionServiceImpl implements DynamicPermissionService {

    private final AppResourceRepository resourceRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuditAccessLogRepository auditAccessLogRepository;
    private final PermissionSecurityService permissionSecurityService;

    // Critical permissions that must never be removed from ROLE_OWNER or ROLE_ADMIN
    private static final Set<String> LOCKED_ADMIN_PERMISSIONS = Set.of(
            "roles_permissions.view",
            "roles_permissions.edit",
            "roles_permissions.create",
            "settings.view",
            "settings.edit"
    );

    @Override
    @Transactional(readOnly = true)
    public List<AppResourceDto> getAllResources() {
        List<AppResource> resources = resourceRepository.findAllByOrderBySortOrderAsc();
        List<Permission> allPermissions = permissionRepository.findAll();

        Map<Long, List<PermissionDto>> permissionsByResourceId = allPermissions.stream()
                .map(p -> PermissionDto.builder()
                        .id(p.getId())
                        .permissionKey(p.getPermissionKey())
                        .resourceKey(p.getResource() != null ? p.getResource().getResourceKey() : null)
                        .action(p.getAction())
                        .displayName(p.getDisplayName())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.groupingBy(p -> {
                    // find resource ID for permission
                    return allPermissions.stream()
                            .filter(perm -> perm.getId().equals(p.getId()))
                            .findFirst()
                            .map(perm -> perm.getResource().getId())
                            .orElse(0L);
                }));

        return resources.stream().map(r -> AppResourceDto.builder()
                .id(r.getId())
                .resourceKey(r.getResourceKey())
                .displayName(r.getDisplayName())
                .category(r.getCategory())
                .route(r.getRoute())
                .description(r.getDescription())
                .sortOrder(r.getSortOrder())
                .active(r.isActive())
                .permissions(permissionsByResourceId.getOrDefault(r.getId(), List.of()))
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleDto(r.getId(), r.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionMatrixDto getRolePermissionsMatrix(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        boolean isSystemAdminRole = "ROLE_OWNER".equalsIgnoreCase(role.getName()) || "ROLE_ADMIN".equalsIgnoreCase(role.getName());

        List<Permission> allPermissions = permissionRepository.findAll();
        List<RolePermission> assignedRolePermissions = rolePermissionRepository.findByRoleIdWithPermissionAndResource(roleId);

        Map<Long, Boolean> grantedMap = assignedRolePermissions.stream()
                .collect(Collectors.toMap(
                        rp -> rp.getPermission().getId(),
                        RolePermission::isGranted,
                        (existing, replacement) -> replacement
                ));

        List<RolePermissionItemDto> items = allPermissions.stream().map(p -> {
            boolean isLocked = isSystemAdminRole && LOCKED_ADMIN_PERMISSIONS.contains(p.getPermissionKey().toLowerCase());
            boolean granted = isLocked || grantedMap.getOrDefault(p.getId(), isSystemAdminRole);

            return RolePermissionItemDto.builder()
                    .permissionId(p.getId())
                    .permissionKey(p.getPermissionKey())
                    .resourceKey(p.getResource() != null ? p.getResource().getResourceKey() : "")
                    .resourceDisplayName(p.getResource() != null ? p.getResource().getDisplayName() : "")
                    .category(p.getResource() != null ? p.getResource().getCategory() : "General")
                    .action(p.getAction())
                    .displayName(p.getDisplayName())
                    .granted(granted)
                    .locked(isLocked)
                    .build();
        }).collect(Collectors.toList());

        return RolePermissionMatrixDto.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .systemRole(isSystemAdminRole)
                .permissions(items)
                .build();
    }

    @Override
    @Transactional
    public void updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request, String actorEmail, String ipAddress) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        boolean isSystemAdminRole = "ROLE_OWNER".equalsIgnoreCase(role.getName()) || "ROLE_ADMIN".equalsIgnoreCase(role.getName());

        List<Permission> allPermissions = permissionRepository.findAll();
        Map<Long, Permission> permById = allPermissions.stream()
                .collect(Collectors.toMap(Permission::getId, p -> p));

        List<RolePermission> existingRolePermissions = rolePermissionRepository.findByRoleId(roleId);
        Map<Long, RolePermission> existingMap = existingRolePermissions.stream()
                .collect(Collectors.toMap(rp -> rp.getPermission().getId(), rp -> rp));

        Set<Long> grantedIds = new HashSet<>();
        if (request.getGrantedPermissionIds() != null) {
            grantedIds.addAll(request.getGrantedPermissionIds());
        } else if (request.getPermissionsMap() != null) {
            request.getPermissionsMap().forEach((permId, granted) -> {
                if (Boolean.TRUE.equals(granted)) {
                    grantedIds.add(permId);
                }
            });
        }

        List<RolePermission> toSave = new ArrayList<>();
        int grantedCount = 0;

        for (Permission perm : allPermissions) {
            boolean shouldGrant = grantedIds.contains(perm.getId());

            // Enforce lockout prevention for Admin/Owner
            if (isSystemAdminRole && LOCKED_ADMIN_PERMISSIONS.contains(perm.getPermissionKey().toLowerCase())) {
                shouldGrant = true;
            }

            if (shouldGrant) {
                grantedCount++;
            }

            RolePermission rp = existingMap.get(perm.getId());
            if (rp == null) {
                rp = RolePermission.builder()
                        .role(role)
                        .permission(perm)
                        .granted(shouldGrant)
                        .updatedBy(actorEmail)
                        .updatedAt(Instant.now())
                        .build();
            } else {
                rp.setGranted(shouldGrant);
                rp.setUpdatedBy(actorEmail);
                rp.setUpdatedAt(Instant.now());
            }
            toSave.add(rp);
        }

        rolePermissionRepository.saveAll(toSave);

        // Invalidate in-memory permission cache immediately
        permissionSecurityService.invalidateCache(roleId);

        // Audit Log
        auditAccessLogRepository.save(AuditAccessLog.builder()
                .actorEmail(actorEmail)
                .actionType("UPDATE_ROLE_PERMISSIONS")
                .target("Role: " + role.getName())
                .details(String.format("Updated permissions for %s. Total granted: %d/%d", role.getName(), grantedCount, allPermissions.size()))
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build());

        log.info("Role permissions updated successfully for role: {} by {}", role.getName(), actorEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccessDto> getAllUsersWithAccess() {
        List<User> users = userRepository.findAll();
        return users.stream().map(u -> {
            Set<String> effectivePerms = permissionSecurityService.getEffectivePermissionsForUser(u);
            Set<RoleDto> roleDtos = u.getRoles().stream()
                    .map(r -> new RoleDto(r.getId(), r.getName()))
                    .collect(Collectors.toSet());

            return UserAccessDto.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .enable(u.isEnabled())
                    .roles(roleDtos)
                    .effectivePermissions(new ArrayList<>(effectivePerms))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, UserRoleAssignmentRequest request, String actorEmail, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<Role> newRoles = roleRepository.findAllById(request.getRoleIds());
        if (newRoles.isEmpty()) {
            throw new IllegalArgumentException("At least one valid role must be selected");
        }

        // Lockout prevention: Check if user was an admin/owner and the change removes admin/owner status
        boolean wasAdminOrOwner = user.getRoles().stream().anyMatch(r ->
                "ROLE_OWNER".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        boolean willBeAdminOrOwner = newRoles.stream().anyMatch(r ->
                "ROLE_OWNER".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        if (wasAdminOrOwner && !willBeAdminOrOwner) {
            // Check how many other enabled admin/owner users exist
            long remainingAdminCount = userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(userId) && u.isEnabled())
                    .filter(u -> u.getRoles().stream().anyMatch(r ->
                            "ROLE_OWNER".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName())))
                    .count();

            if (remainingAdminCount < 1) {
                throw new IllegalStateException("Cannot revoke administrator privilege: at least one other active Administrator or Owner must exist.");
            }
        }

        user.setRoles(new HashSet<>(newRoles));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Audit Log
        String roleNames = newRoles.stream().map(Role::getName).collect(Collectors.joining(", "));
        auditAccessLogRepository.save(AuditAccessLog.builder()
                .actorEmail(actorEmail)
                .actionType("ASSIGN_USER_ROLES")
                .target("User: " + user.getEmail())
                .details("Assigned roles: [" + roleNames + "]")
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build());

        log.info("Roles updated for user: {} to [{}] by {}", user.getEmail(), roleNames, actorEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditAccessLogDto> getAuditLogs() {
        return auditAccessLogRepository.findTop100ByOrderByTimestampDesc().stream()
                .map(log -> AuditAccessLogDto.builder()
                        .id(log.getId())
                        .actorEmail(log.getActorEmail())
                        .actionType(log.getActionType())
                        .target(log.getTarget())
                        .details(log.getDetails())
                        .ipAddress(log.getIpAddress())
                        .timestamp(log.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }
}
