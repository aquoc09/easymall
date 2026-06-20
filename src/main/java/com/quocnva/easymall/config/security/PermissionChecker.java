package com.quocnva.easymall.config.security;

import com.quocnva.easymall.entity.RoleEntity;
import com.quocnva.easymall.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Called via SpEL in @PreAuthorize annotations:
 * {@code @PreAuthorize("@permissionChecker.has('user:read')")}
 *
 * Resolves the current user's role from SecurityContext,
 * then queries DB for that role's permissions to authorize the action.
 */
@Component("permissionChecker")
@RequiredArgsConstructor
public class PermissionChecker {

    private final RoleRepository roleRepository;

    /**
     * Checks if the currently authenticated user's role
     * has the specified permission.
     *
     * @param requiredPermission e.g. "user:read", "role:create"
     * @return true if the role has the permission, false otherwise
     */
    public boolean has(String requiredPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return false;
        }

        String roleName = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null);

        if (roleName == null) {
            return false;
        }

        return roleRepository.findByRoleNameWithPermissions(roleName)
                .map(RoleEntity::getPermissions)
                .map(permissions -> permissions.stream()
                        .anyMatch(p -> p.getPermissionName().equals(requiredPermission)))
                .orElse(false);
    }
}
