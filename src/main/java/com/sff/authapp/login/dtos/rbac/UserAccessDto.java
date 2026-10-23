package com.sff.authapp.login.dtos.rbac;

import com.sff.authapp.login.dtos.RoleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private boolean enable;
    private Set<RoleDto> roles;
    private List<String> effectivePermissions;
}
