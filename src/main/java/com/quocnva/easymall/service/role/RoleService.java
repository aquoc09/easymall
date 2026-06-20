package com.quocnva.easymall.service.role;

import com.quocnva.easymall.dtos.request.role.CreateRoleRequest;
import com.quocnva.easymall.dtos.request.role.UpdateRoleRequest;
import com.quocnva.easymall.dtos.response.role.RoleResponse;

import java.util.List;

/**
 * Role management service interface.
 * Encapsulates: list roles, create role, update role, delete role, assign/revoke permissions.
 */
public interface RoleService {

    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(Long id);

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse updateRole(Long id, UpdateRoleRequest request);

    void deleteRole(Long id);
}
