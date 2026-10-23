package com.sff.authapp.login.dtos.rbac;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionsRequest {
    /**
     * Map of permissionId -> granted status (true/false)
     * OR list of granted permission IDs.
     */
    private Map<Long, Boolean> permissionsMap;
    private List<Long> grantedPermissionIds;
}
