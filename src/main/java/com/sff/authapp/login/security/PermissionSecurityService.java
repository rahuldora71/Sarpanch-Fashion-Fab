package com.sff.authapp.login.security;

import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.RolePermission;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service("perm")
@RequiredArgsConstructor
public class PermissionSecurityService {

    private final RolePermissionRepository rolePermissionRepository;

    // Cache of roleId -> Set of lowercase permission keys (e.g. "orders.view", "orders:view")
    private final Map<Long, Set<String>> rolePermissionsCache = new ConcurrentHashMap<>();

    public boolean can(String resource, String action) {
        if (resource == null || action == null) {
            return false;
        }
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }

        // ROLE_OWNER and ROLE_ADMIN have global access by default to avoid accidental lockouts
        if (isAdmin(user)) {
            return true;
        }

        String targetKey1 = (resource + "." + action).toLowerCase();
        String targetKey2 = (resource + ":" + action).toLowerCase();

        Set<Long> roleIds = user.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        for (Long roleId : roleIds) {
            Set<String> perms = getPermissionsForRole(roleId);
            if (perms.contains(targetKey1) || perms.contains(targetKey2) || perms.contains((resource + ".*").toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean can(String permissionKey) {
        if (permissionKey == null) return false;
        String[] parts = permissionKey.contains(":") ? permissionKey.split(":") : permissionKey.split("\\.");
        if (parts.length >= 2) {
            return can(parts[0], parts[1]);
        }
        User user = getCurrentUser();
        if (user == null) return false;
        if (isAdmin(user)) return true;

        String key = permissionKey.toLowerCase();
        for (Role role : user.getRoles()) {
            if (getPermissionsForRole(role.getId()).contains(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin() {
        User user = getCurrentUser();
        return isAdmin(user);
    }

    public boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r ->
                "ROLE_OWNER".equalsIgnoreCase(r.getName()) ||
                "ROLE_ADMIN".equalsIgnoreCase(r.getName()) ||
                "OWNER".equalsIgnoreCase(r.getName()) ||
                "ADMIN".equalsIgnoreCase(r.getName())
        );
    }

    public boolean isCurrentUser(Long targetUserId) {
        User user = getCurrentUser();
        return user != null && user.getId() != null && user.getId().equals(targetUserId);
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }

    public Set<String> getPermissionsForRole(Long roleId) {
        return rolePermissionsCache.computeIfAbsent(roleId, this::loadRolePermissionsFromDb);
    }

    public Set<String> getEffectivePermissionsForUser(User user) {
        if (user == null || user.getRoles() == null) {
            return Collections.emptySet();
        }
        if (isAdmin(user)) {
            // Admins get wildcard or all loaded permissions
            Set<String> all = new HashSet<>();
            rolePermissionsCache.values().forEach(all::addAll);
            if (all.isEmpty()) {
                all.add("*");
            }
            return all;
        }
        Set<String> effective = new HashSet<>();
        for (Role role : user.getRoles()) {
            effective.addAll(getPermissionsForRole(role.getId()));
        }
        return effective;
    }

    public void invalidateCache(Long roleId) {
        if (roleId == null) {
            rolePermissionsCache.clear();
        } else {
            rolePermissionsCache.remove(roleId);
        }
        log.info("Role permission cache invalidated for roleId: {}", roleId);
    }

    public void invalidateAll() {
        rolePermissionsCache.clear();
        log.info("Role permission cache cleared");
    }

    private Set<String> loadRolePermissionsFromDb(Long roleId) {
        List<RolePermission> rps = rolePermissionRepository.findByRoleIdWithPermissionAndResource(roleId);
        Set<String> perms = new HashSet<>();
        for (RolePermission rp : rps) {
            if (rp.isGranted() && rp.getPermission() != null) {
                String key = rp.getPermission().getPermissionKey().toLowerCase();
                perms.add(key);
                if (rp.getPermission().getResource() != null && rp.getPermission().getAction() != null) {
                    String resKey = rp.getPermission().getResource().getResourceKey().toLowerCase();
                    String action = rp.getPermission().getAction().toLowerCase();
                    perms.add(resKey + "." + action);
                    perms.add(resKey + ":" + action);
                }
            }
        }
        return perms;
    }
}
