package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.rbac.UpdateRolePermissionsRequest;
import com.sff.authapp.login.dtos.rbac.UserRoleAssignmentRequest;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.security.PermissionSecurityService;
import com.sff.authapp.login.services.impl.DynamicPermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicPermissionServiceTest {

    @Mock
    private AppResourceRepository resourceRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditAccessLogRepository auditAccessLogRepository;

    @Mock
    private PermissionSecurityService permissionSecurityService;

    @InjectMocks
    private DynamicPermissionServiceImpl dynamicPermissionService;

    private Role adminRole;
    private Role workerRole;
    private User adminUser;
    private Permission permRolesView;
    private Permission permOrdersView;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().id(1L).name("ROLE_ADMIN").build();
        workerRole = Role.builder().id(2L).name("ROLE_WORKER").build();

        adminUser = User.builder()
                .id(10L)
                .email("admin@sff.com")
                .name("Admin User")
                .roles(new HashSet<>(Set.of(adminRole)))
                .enable(true)
                .build();

        AppResource rolesRes = AppResource.builder().id(1L).resourceKey("roles_permissions").displayName("Roles").build();
        AppResource ordersRes = AppResource.builder().id(2L).resourceKey("orders").displayName("Orders").build();

        permRolesView = Permission.builder().id(101L).permissionKey("roles_permissions.view").action("VIEW").resource(rolesRes).build();
        permOrdersView = Permission.builder().id(102L).permissionKey("orders.view").action("VIEW").resource(ordersRes).build();
    }

    @Test
    @DisplayName("Lockout Prevention: Locked admin permissions remain granted when updating ROLE_ADMIN")
    void testUpdateRolePermissions_LockedAdminPermissionsStayGranted() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(permissionRepository.findAll()).thenReturn(List.of(permRolesView, permOrdersView));
        when(rolePermissionRepository.findByRoleId(1L)).thenReturn(Collections.emptyList());

        // Attempt to revoke all permissions (grant empty list)
        UpdateRolePermissionsRequest request = UpdateRolePermissionsRequest.builder()
                .grantedPermissionIds(Collections.emptyList())
                .build();

        dynamicPermissionService.updateRolePermissions(1L, request, "superadmin@sff.com", "127.0.0.1");

        // Verify rolePermissionRepository.saveAll was called
        verify(rolePermissionRepository).saveAll(argThat(iterable -> {
            List<RolePermission> list = new ArrayList<>();
            iterable.forEach(list::add);
            // permRolesView must be TRUE because it is locked for ROLE_ADMIN
            boolean rolesViewGranted = list.stream()
                    .anyMatch(rp -> rp.getPermission().getId().equals(101L) && rp.isGranted());
            // permOrdersView must be FALSE because it was not in granted list
            boolean ordersViewGranted = list.stream()
                    .anyMatch(rp -> rp.getPermission().getId().equals(102L) && rp.isGranted());

            return rolesViewGranted && !ordersViewGranted;
        }));

        verify(permissionSecurityService).invalidateCache(1L);
    }

    @Test
    @DisplayName("Lockout Prevention: Cannot remove ROLE_ADMIN from last remaining admin user")
    void testAssignUserRoles_PreventLockoutLastAdmin() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of(workerRole));
        // Only one admin exists
        when(userRepository.findAll()).thenReturn(List.of(adminUser));

        UserRoleAssignmentRequest request = UserRoleAssignmentRequest.builder()
                .roleIds(List.of(2L)) // switch admin to worker
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                dynamicPermissionService.assignUserRoles(10L, request, "actor@sff.com", "127.0.0.1")
        );

        assertTrue(exception.getMessage().contains("Cannot revoke administrator privilege"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign Roles: Successfully assign new role when another admin exists")
    void testAssignUserRoles_SuccessWhenMultipleAdminsExist() {
        User anotherAdmin = User.builder()
                .id(20L)
                .email("admin2@sff.com")
                .roles(new HashSet<>(Set.of(adminRole)))
                .enable(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of(workerRole));
        when(userRepository.findAll()).thenReturn(List.of(adminUser, anotherAdmin));

        UserRoleAssignmentRequest request = UserRoleAssignmentRequest.builder()
                .roleIds(List.of(2L))
                .build();

        dynamicPermissionService.assignUserRoles(10L, request, "admin2@sff.com", "127.0.0.1");

        verify(userRepository).save(argThat(user ->
                user.getRoles().contains(workerRole) && !user.getRoles().contains(adminRole)
        ));
    }
}
