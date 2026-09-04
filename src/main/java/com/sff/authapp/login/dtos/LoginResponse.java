package com.sff.authapp.login.dtos;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.entities.Provider;

import java.time.Instant;
import java.util.Set;

public record LoginResponse(
        Long id,
        String email,
        String name,
        String image,
        boolean enable,
        Instant createdAt,
        Instant updatedAt,
        Provider provider,
        Set<RoleDto> roles
) {
}
