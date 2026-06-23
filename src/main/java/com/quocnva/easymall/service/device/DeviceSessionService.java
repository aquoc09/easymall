package com.quocnva.easymall.service.device;

import com.quocnva.easymall.entity.DeviceSessionEntity;
import com.quocnva.easymall.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;

public interface DeviceSessionService {

    /**
     * Tự động tạo hoặc tái sử dụng DeviceSession từ thông tin request.
     * Server extract ip/ua/fingerprint — client không truyền deviceSessionId.
     *
     * @param request  HTTP request của checkout
     * @param user     User đang thực hiện checkout
     * @return DeviceSessionEntity đã được persist
     */
    DeviceSessionEntity getOrCreate(HttpServletRequest request, UserEntity user);
}
