package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.RoleDto;
import com.sff.authapp.login.dtos.role.RoleCreateRequest;

public interface RoleService {

    RoleDto createRole(RoleCreateRequest request);
}