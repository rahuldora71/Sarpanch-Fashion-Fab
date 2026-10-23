package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.profile.ChangePasswordRequest;
import com.sff.authapp.login.dtos.profile.UpdateProfileRequest;
import com.sff.authapp.login.dtos.profile.UserProfileResponse;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.AuditAccessLogRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.security.PermissionSecurityService;
import com.sff.authapp.login.services.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PermissionSecurityService permissionSecurityService;

    @Mock
    private AuditAccessLogRepository auditAccessLogRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(1L).name("ROLE_ADMIN").build();
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Old Name")
                .password("encodedOldPassword")
                .phone("9876543210")
                .roles(new HashSet<>(Set.of(role)))
                .enable(true)
                .build();
    }

    @Test
    @DisplayName("Get Profile: Returns profile and effective permissions")
    void testGetProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(permissionSecurityService.getEffectivePermissionsForUser(testUser))
                .thenReturn(Set.of("orders.view", "billing.view"));

        UserProfileResponse response = profileService.getProfile(testUser);

        assertNotNull(response);
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Old Name", response.getName());
        assertEquals(2, response.getEffectivePermissions().size());
    }

    @Test
    @DisplayName("Update Profile: Updates permitted profile fields")
    void testUpdateProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionSecurityService.getEffectivePermissionsForUser(any(User.class)))
                .thenReturn(Collections.emptySet());

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("New Name")
                .phone("1122334455")
                .city("Ahmedabad")
                .build();

        UserProfileResponse response = profileService.updateProfile(testUser, request);

        assertEquals("New Name", response.getName());
        assertEquals("1122334455", response.getPhone());
        assertEquals("Ahmedabad", response.getCity());
        verify(auditAccessLogRepository).save(any());
    }

    @Test
    @DisplayName("Change Password: Successfully changes when current password matches")
    void testChangePassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123!", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();

        profileService.changePassword(testUser, request, "127.0.0.1");

        assertEquals("encodedNewPassword", testUser.getPassword());
        verify(userRepository).save(testUser);
        verify(auditAccessLogRepository).save(any());
    }

    @Test
    @DisplayName("Change Password: Fails when current password does not match")
    void testChangePassword_WrongCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", "encodedOldPassword")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                profileService.changePassword(testUser, request, "127.0.0.1")
        );

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Change Password: Fails when new password matches current password")
    void testChangePassword_SameAsCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123!", "encodedOldPassword")).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("OldPassword123!")
                .confirmPassword("OldPassword123!")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                profileService.changePassword(testUser, request, "127.0.0.1")
        );

        assertEquals("New password cannot be the same as current password", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}
