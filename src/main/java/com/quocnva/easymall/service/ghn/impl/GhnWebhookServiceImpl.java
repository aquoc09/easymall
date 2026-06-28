package com.quocnva.easymall.service.ghn.impl;

import com.quocnva.easymall.dtos.request.ghn.GhnWebhookRequest;
import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.repository.OrderRepository;
import com.quocnva.easymall.service.ghn.GhnWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnWebhookServiceImpl implements GhnWebhookService {

    private final OrderRepository orderRepository;

    /**
     * Mapping GHN status → deliveryStatus (lưu vào orders.delivery_status)
     * và cập nhật order_status khi cần thiết.
     *
     * GHN Status codes:
     *   ready_to_pick → READY_TO_PICK
     *   picking       → PICKING
     *   delivering    → DELIVERING
     *   delivered     → DELIVERED        (→ OrderStatus.COMPLETED)
     *   delivery_fail → DELIVERY_FAIL
     *   return        → RETURNING
     *   returned      → RETURNED
     *   cancel        → CANCELLED        (→ OrderStatus.CANCELLED)
     */
    @Override
    @Transactional
    public void handleWebhook(GhnWebhookRequest request) {
        String ghnStatus = request.getStatus();
        String orderCode = request.getOrderCode();

        log.info("[GHN Webhook] orderCode={} status={}", orderCode, ghnStatus);

        OrderEntity order = orderRepository.findByTrackingNumber(orderCode)
                .orElseGet(() -> {
                    log.warn("[GHN Webhook] Unknown orderCode={}", orderCode);
                    return null;
                });

        if (order == null) return;

        // Cập nhật delivery_status
        String deliveryStatus = mapGhnStatus(ghnStatus);
        order.setDeliveryStatus(deliveryStatus);

        // Cập nhật order_status khi cần
        switch (ghnStatus.toLowerCase()) {
            case "delivered" -> order.setOrderStatus(OrderStatus.COMPLETED);
            case "cancel" -> order.setOrderStatus(OrderStatus.CANCELLED);
            default -> { /* không thay đổi order_status */ }
        }

        orderRepository.save(order);
        log.info("[GHN Webhook] Updated order={} deliveryStatus={} orderStatus={}",
                order.getOrderId(), deliveryStatus, order.getOrderStatus());
    }

    private String mapGhnStatus(String ghnStatus) {
        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick"   -> "READY_TO_PICK";
            case "picking"         -> "PICKING";
            case "delivering"      -> "DELIVERING";
            case "delivered"       -> "DELIVERED";
            case "delivery_fail"   -> "DELIVERY_FAIL";
            case "return"          -> "RETURNING";
            case "returned"        -> "RETURNED";
            case "cancel"          -> "CANCELLED";
            default                -> ghnStatus.toUpperCase();
        };
    }
}
