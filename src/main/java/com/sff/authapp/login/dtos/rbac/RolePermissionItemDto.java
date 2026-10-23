package com.sff.authapp.login.dtos.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionItemDto {
    private Long permissionId;
    private String permissionKey;
    private String resourceKey;
    private String resourceDisplayName;
    private String category;
    private String action;
    private String displayName;
    private boolean granted;
    private boolean locked; // true if cannot be disabled (e.g. admin role permissions)
}
