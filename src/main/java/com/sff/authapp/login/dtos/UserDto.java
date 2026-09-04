package com.sff.authapp.login.dtos;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.entities.Provider;

import java.time.Instant;
import java.util.Set;


public record UserDto(
        Long id,
        String email,
        String name,
        String password,
        String image,
        boolean enable,
        Instant createdAt,
        Instant updatedAt,
        Provider provider,
        Set<RoleDto> roles
) {
}
