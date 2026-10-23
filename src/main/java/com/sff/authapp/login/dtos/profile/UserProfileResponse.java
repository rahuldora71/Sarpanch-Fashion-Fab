package com.sff.authapp.login.dtos.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String notes;
    private String image;
    private String workerSpecialty;
    private java.math.BigDecimal dailyCapacity;
    private List<String> roles;
    private List<String> effectivePermissions;
    private Instant createdAt;
    private Instant updatedAt;
}
