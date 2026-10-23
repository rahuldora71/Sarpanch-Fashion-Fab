package com.sff.authapp.login.dtos.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAccessLogDto {
    private Long id;
    private String actorEmail;
    private String actionType;
    private String target;
    private String details;
    private String ipAddress;
    private Instant timestamp;
}
