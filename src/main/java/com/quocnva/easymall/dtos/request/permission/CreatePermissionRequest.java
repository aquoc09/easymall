package com.quocnva.easymall.dtos.request.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePermissionRequest {

    @NotBlank(message = "{validation.permissionName.not-blank}")
    private String permissionName;

    private String description;
}
