package com.quocnva.easymall.dtos.response.role;

import com.quocnva.easymall.dtos.response.permission.PermissionResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Set;

@Getter
@Builder
public class RoleResponse {

    private Long roleId;
    private String roleName;
    private String description;
    private OffsetDateTime createdAt;
    private Set<PermissionResponse> permissions;
}
