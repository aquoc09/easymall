package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByPermissionName(String permissionName);

    boolean existsByPermissionName(String permissionName);

    Set<PermissionEntity> findAllByPermissionIdIn(Set<Long> ids);
}
