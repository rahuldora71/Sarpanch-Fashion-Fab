package com.sff.authapp.login.dtos.rbac;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAssignmentRequest {
    @NotEmpty(message = "At least one role ID must be specified")
    private List<Long> roleIds;
}
