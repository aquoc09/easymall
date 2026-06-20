package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    @Query("SELECT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.roleName = :roleName")
    Optional<RoleEntity> findByRoleNameWithPermissions(@Param("roleName") String roleName);

    @Query("SELECT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.roleId = :id")
    Optional<RoleEntity> findByIdWithPermissions(@Param("id") Long id);
}
