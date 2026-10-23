package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.profile.ChangePasswordRequest;
import com.sff.authapp.login.dtos.profile.UpdateProfileRequest;
import com.sff.authapp.login.dtos.profile.UserProfileResponse;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.security.PermissionSecurityService;
import com.sff.authapp.login.services.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final PermissionSecurityService permissionSecurityService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        User currentUser = permissionSecurityService.getCurrentUser();
        return ResponseEntity.ok(profileService.getProfile(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User currentUser = permissionSecurityService.getCurrentUser();
        return ResponseEntity.ok(profileService.updateProfile(currentUser, request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        User currentUser = permissionSecurityService.getCurrentUser();
        String ip = servletRequest.getRemoteAddr();
        profileService.changePassword(currentUser, request, ip);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<String>> getCurrentUserPermissions() {
        User currentUser = permissionSecurityService.getCurrentUser();
        return ResponseEntity.ok(profileService.getEffectivePermissions(currentUser));
    }
}
