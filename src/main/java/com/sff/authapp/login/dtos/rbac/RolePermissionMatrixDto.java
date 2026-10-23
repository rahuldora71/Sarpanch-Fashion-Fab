package com.sff.authapp.login.dtos.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionMatrixDto {
    private Long roleId;
    private String roleName;
    private boolean systemRole;
    private List<RolePermissionItemDto> permissions;
}
