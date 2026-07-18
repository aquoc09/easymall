package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.DeviceSessionEntity;
import com.quocnva.easymall.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceSessionRepository extends JpaRepository<DeviceSessionEntity, Long> {

    Optional<DeviceSessionEntity> findBySessionId(String sessionId);

    Optional<DeviceSessionEntity> findByUserAndSessionId(UserEntity user, String sessionId);

    @Query("SELECT COUNT(DISTINCT d.deviceFingerprint) FROM DeviceSessionEntity d WHERE d.user.userId = :userId AND d.createdAt >= :since")
    long countDistinctDevicesByUserSince(@Param("userId") Long userId, @Param("since") OffsetDateTime since);
}
