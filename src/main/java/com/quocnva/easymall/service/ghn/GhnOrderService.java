package com.quocnva.easymall.service.ghn;

import com.quocnva.easymall.dtos.response.ghn.GhnCreateOrderResponse;
import com.quocnva.easymall.entity.OrderEntity;

public interface GhnOrderService {

    /**
     * Tạo đơn hàng trên GHN khi order chuyển sang SHIPPING.
     * Trả về orderCode (= tracking_number) và totalFee.
     */
    GhnCreateOrderResponse createOrder(OrderEntity order);
}
