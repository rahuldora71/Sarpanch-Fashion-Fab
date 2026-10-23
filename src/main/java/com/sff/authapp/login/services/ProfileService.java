package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.profile.ChangePasswordRequest;
import com.sff.authapp.login.dtos.profile.UpdateProfileRequest;
import com.sff.authapp.login.dtos.profile.UserProfileResponse;
import com.sff.authapp.login.entities.User;

import java.util.List;

public interface ProfileService {

    UserProfileResponse getProfile(User currentUser);

    UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request);

    void changePassword(User currentUser, ChangePasswordRequest request, String ipAddress);

    List<String> getEffectivePermissions(User currentUser);
}
