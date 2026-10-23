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
public class AppResourceDto {
    private Long id;
    private String resourceKey;
    private String displayName;
    private String category;
    private String route;
    private String description;
    private Integer sortOrder;
    private boolean active;
    private List<PermissionDto> permissions;
}
