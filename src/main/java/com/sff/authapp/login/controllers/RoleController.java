package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.dtos.role.RoleCreateRequest;
import com.sff.authapp.login.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RoleDto> createRole(
            @Valid @RequestBody RoleCreateRequest request
    ) {

        RoleDto response =
                roleService.createRole(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}