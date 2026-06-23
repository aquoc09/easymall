package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.DeviceSessionEntity;
import com.quocnva.easymall.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceSessionRepository extends JpaRepository<DeviceSessionEntity, Long> {

    Optional<DeviceSessionEntity> findBySessionId(String sessionId);

    Optional<DeviceSessionEntity> findByUserAndSessionId(UserEntity user, String sessionId);
}
