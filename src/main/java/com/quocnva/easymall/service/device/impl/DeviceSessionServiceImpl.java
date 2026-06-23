package com.quocnva.easymall.service.device.impl;

import com.quocnva.easymall.entity.DeviceSessionEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.repository.DeviceSessionRepository;
import com.quocnva.easymall.service.device.DeviceSessionService;
import com.quocnva.easymall.util.DeviceSessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceSessionServiceImpl implements DeviceSessionService {

    private final DeviceSessionRepository deviceSessionRepository;

    @Override
    @Transactional
    public DeviceSessionEntity getOrCreate(HttpServletRequest request, UserEntity user) {
        String ip          = DeviceSessionUtil.extractIp(request);
        String userAgent   = DeviceSessionUtil.extractUserAgent(request);
        String fingerprint = DeviceSessionUtil.extractFingerprint(request);
        String sessionId   = DeviceSessionUtil.buildSessionId(ip, userAgent);

        return deviceSessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    DeviceSessionEntity session = DeviceSessionEntity.builder()
                            .user(user)
                            .sessionId(sessionId)
                            .ipAddress(ip)
                            .userAgent(userAgent)
                            .deviceFingerprint(fingerprint)
                            .build();
                    return deviceSessionRepository.save(session);
                });
    }
}
