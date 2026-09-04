package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.dtos.role.RoleCreateRequest;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.repositories.RoleRepository;
import com.sff.authapp.login.services.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public RoleDto createRole(RoleCreateRequest request) {

        String roleName = request.name().trim().toUpperCase();

        // Make sure role doesn't already exist
        if (roleRepository.existsByName(roleName)) {
            throw new IllegalArgumentException(
                    "Role already exists: " + roleName
            );
        }

        Role role = Role.builder()
                .name(roleName)
                .build();

        Role savedRole = roleRepository.save(role);

        return new RoleDto(
                savedRole.getId(),
                savedRole.getName()
        );
    }
}