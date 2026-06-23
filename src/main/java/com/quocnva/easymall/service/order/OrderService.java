package com.quocnva.easymall.service.order;

import com.quocnva.easymall.dtos.request.order.CheckoutRequest;
import com.quocnva.easymall.dtos.request.order.OrderCancelRequest;
import com.quocnva.easymall.dtos.request.order.OrderStatusUpdateRequest;
import com.quocnva.easymall.dtos.response.order.CheckoutResponse;
import com.quocnva.easymall.dtos.response.order.OrderResponse;
import com.quocnva.easymall.dtos.response.order.OrderSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    // ── USER ──────────────────────────────────────────────────────────────

    CheckoutResponse checkout(CheckoutRequest request, String userEmail, HttpServletRequest httpRequest);

    Page<OrderSummaryResponse> getMyOrders(String userEmail, Pageable pageable);

    OrderResponse getMyOrderDetail(Long orderId, String userEmail);

    void cancelMyOrder(Long orderId, OrderCancelRequest request, String userEmail);

    // ── ADMIN ─────────────────────────────────────────────────────────────

    Page<OrderSummaryResponse> getAllOrders(Pageable pageable);

    OrderResponse getOrderDetail(Long orderId);

    void updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);
}
