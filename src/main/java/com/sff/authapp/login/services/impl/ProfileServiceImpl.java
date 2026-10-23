package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.profile.ChangePasswordRequest;
import com.sff.authapp.login.dtos.profile.UpdateProfileRequest;
import com.sff.authapp.login.dtos.profile.UserProfileResponse;
import com.sff.authapp.login.entities.AuditAccessLog;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.AuditAccessLogRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.security.PermissionSecurityService;
import com.sff.authapp.login.services.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionSecurityService permissionSecurityService;
    private final AuditAccessLogRepository auditAccessLogRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + currentUser.getId()));

        Set<String> effectivePerms = permissionSecurityService.getEffectivePermissionsForUser(user);

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .alternatePhone(user.getAlternatePhone())
                .address(user.getAddress())
                .city(user.getCity())
                .notes(user.getNotes())
                .image(user.getImage())
                .workerSpecialty(user.getWorkerSpecialty())
                .dailyCapacity(user.getDailyCapacity())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .effectivePermissions(new ArrayList<>(effectivePerms))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + currentUser.getId()));

        user.setName(request.getName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setAlternatePhone(request.getAlternatePhone() != null ? request.getAlternatePhone().trim() : null);
        user.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        user.setCity(request.getCity() != null ? request.getCity().trim() : null);
        user.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        if (request.getImage() != null && !request.getImage().isBlank()) {
            user.setImage(request.getImage().trim());
        }
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);

        auditAccessLogRepository.save(AuditAccessLog.builder()
                .actorEmail(saved.getEmail())
                .actionType("UPDATE_PROFILE")
                .target("User: " + saved.getEmail())
                .details("Profile details updated successfully")
                .timestamp(Instant.now())
                .build());

        Set<String> effectivePerms = permissionSecurityService.getEffectivePermissionsForUser(saved);

        return UserProfileResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .name(saved.getName())
                .phone(saved.getPhone())
                .alternatePhone(saved.getAlternatePhone())
                .address(saved.getAddress())
                .city(saved.getCity())
                .notes(saved.getNotes())
                .image(saved.getImage())
                .workerSpecialty(saved.getWorkerSpecialty())
                .dailyCapacity(saved.getDailyCapacity())
                .roles(saved.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .effectivePermissions(new ArrayList<>(effectivePerms))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request, String ipAddress) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + currentUser.getId()));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Prevent reusing the exact same password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditAccessLogRepository.save(AuditAccessLog.builder()
                .actorEmail(user.getEmail())
                .actionType("CHANGE_PASSWORD")
                .target("User: " + user.getEmail())
                .details("Password changed successfully by user")
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build());

        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getEffectivePermissions(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return List.of();
        }
        User user = userRepository.findById(currentUser.getId()).orElse(currentUser);
        return new ArrayList<>(permissionSecurityService.getEffectivePermissionsForUser(user));
    }
}
