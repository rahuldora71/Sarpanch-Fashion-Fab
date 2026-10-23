package com.sff.authapp.login.security;

import com.sff.authapp.login.entities.AppResource;
import com.sff.authapp.login.entities.Permission;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.RolePermission;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.RolePermissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionSecurityServiceTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private PermissionSecurityService permissionSecurityService;

    private User adminUser;
    private User workerUser;
    private Role roleAdmin;
    private Role roleWorker;

    @BeforeEach
    void setUp() {
        roleAdmin = Role.builder().id(1L).name("ROLE_ADMIN").build();
        roleWorker = Role.builder().id(2L).name("ROLE_WORKER").build();

        adminUser = User.builder()
                .id(100L)
                .email("admin@test.com")
                .roles(Set.of(roleAdmin))
                .build();

        workerUser = User.builder()
                .id(200L)
                .email("worker@test.com")
                .roles(Set.of(roleWorker))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Admin user automatically has permission to everything")
    void testCan_AdminUserHasAccess() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities())
        );

        assertTrue(permissionSecurityService.can("orders", "DELETE"));
        assertTrue(permissionSecurityService.can("billing", "PRINT"));
        assertTrue(permissionSecurityService.can("roles_permissions", "EDIT"));
    }

    @Test
    @DisplayName("Worker user has permission when role has granted = true")
    void testCan_WorkerUserHasGrantedPermission() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(workerUser, null, workerUser.getAuthorities())
        );

        AppResource cuttingRes = AppResource.builder().resourceKey("cutting").build();
        Permission cuttingView = Permission.builder().permissionKey("cutting.view").resource(cuttingRes).action("VIEW").build();
        RolePermission rp = RolePermission.builder().role(roleWorker).permission(cuttingView).granted(true).build();

        when(rolePermissionRepository.findByRoleIdWithPermissionAndResource(2L)).thenReturn(List.of(rp));

        assertTrue(permissionSecurityService.can("cutting", "VIEW"));
        // orders DELETE is not granted
        assertFalse(permissionSecurityService.can("orders", "DELETE"));
    }

    @Test
    @DisplayName("Cache Invalidation: Updates to role permissions take effect after cache eviction")
    void testInvalidateCache() {
        AppResource cuttingRes = AppResource.builder().resourceKey("cutting").build();
        Permission cuttingView = Permission.builder().permissionKey("cutting.view").resource(cuttingRes).action("VIEW").build();
        RolePermission rp = RolePermission.builder().role(roleWorker).permission(cuttingView).granted(true).build();

        when(rolePermissionRepository.findByRoleIdWithPermissionAndResource(2L)).thenReturn(List.of(rp));

        // First call loads into cache
        Set<String> perms = permissionSecurityService.getPermissionsForRole(2L);
        assertTrue(perms.contains("cutting.view"));

        // Cache hit does not call repository again
        permissionSecurityService.getPermissionsForRole(2L);
        verify(rolePermissionRepository, times(1)).findByRoleIdWithPermissionAndResource(2L);

        // Invalidate cache
        permissionSecurityService.invalidateCache(2L);

        // Next call re-queries DB
        permissionSecurityService.getPermissionsForRole(2L);
        verify(rolePermissionRepository, times(2)).findByRoleIdWithPermissionAndResource(2L);
    }
}
