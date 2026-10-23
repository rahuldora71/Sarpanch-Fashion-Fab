package com.sff.authapp.login.dtos.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private Long id;
    private String permissionKey;
    private String resourceKey;
    private String action;
    private String displayName;
    private String description;
}
