package com.quocnva.easymall.dtos.request.role;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateRoleRequest {

    private String description;

    @NotNull(message = "{validation.permissionIds.not-null}")
    private Set<Long> permissionIds;
}
