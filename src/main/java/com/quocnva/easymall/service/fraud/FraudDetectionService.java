package com.quocnva.easymall.service.fraud;

import com.quocnva.easymall.entity.DeviceSessionEntity;
import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.SystemDecision;

public interface FraudDetectionService {

    /**
     * Đánh giá rủi ro gian lận của một đơn hàng.
     * @param order Đối tượng OrderEntity đã được map dữ liệu tài chính
     * @param deviceSession Thông tin thiết bị
     * @param user Thông tin người dùng
     * @return SystemDecision: APPROVE, REVIEW, hoặc DECLINE
     */
    SystemDecision evaluateOrder(OrderEntity order, DeviceSessionEntity deviceSession, UserEntity user);
}
