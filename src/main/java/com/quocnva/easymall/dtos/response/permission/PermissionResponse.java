package com.quocnva.easymall.dtos.response.permission;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class PermissionResponse {

    private Long permissionId;
    private String permissionName;
    private String description;
    private OffsetDateTime createdAt;
}
