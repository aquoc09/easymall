package com.quocnva.easymall.controller;

import com.quocnva.easymall.util.Translator;

import com.quocnva.easymall.dtos.request.order.CheckoutRequest;
import com.quocnva.easymall.dtos.request.order.OrderCancelRequest;
import com.quocnva.easymall.dtos.request.order.OrderStatusUpdateRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.order.CheckoutResponse;
import com.quocnva.easymall.dtos.response.order.OrderResponse;
import com.quocnva.easymall.dtos.response.order.OrderSummaryResponse;
import com.quocnva.easymall.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ── USER ───────────────────────────────────────────────────────────────

    /**
     * Đặt hàng (checkout).
     * HttpServletRequest được inject tự động bởi Spring — dùng để extract DeviceSession.
     */
    @PostMapping("/checkout")
    @PreAuthorize("@permissionChecker.has('order:create')")
    public ApiResponse<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.<CheckoutResponse>builder()
                .result(orderService.checkout(request, authentication.getName(), httpRequest))
                .message(Translator.toLocale("success.order.placed"))
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize("@permissionChecker.has('order:view')")
    public ApiResponse<Page<OrderSummaryResponse>> getMyOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "orderId") Pageable pageable) {
        return ApiResponse.<Page<OrderSummaryResponse>>builder()
                .result(orderService.getMyOrders(authentication.getName(), pageable))
                .build();
    }

    @GetMapping("/my/{orderId}")
    @PreAuthorize("@permissionChecker.has('order:view')")
    public ApiResponse<OrderResponse> getMyOrderDetail(
            @PathVariable Long orderId,
            Authentication authentication) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getMyOrderDetail(orderId, authentication.getName()))
                .build();
    }

    @PutMapping("/my/{orderId}/cancel")
    @PreAuthorize("@permissionChecker.has('order:manage')")
    public ApiResponse<Void> cancelMyOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancelRequest request,
            Authentication authentication) {
        orderService.cancelMyOrder(orderId, request, authentication.getName());
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.order.cancelled"))
                .build();
    }

    // ── ADMIN ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("@permissionChecker.has('order:admin')")
    public ApiResponse<Page<OrderSummaryResponse>> getAllOrders(
            @PageableDefault(size = 20, sort = "orderId") Pageable pageable) {
        return ApiResponse.<Page<OrderSummaryResponse>>builder()
                .result(orderService.getAllOrders(pageable))
                .build();
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("@permissionChecker.has('order:admin')")
    public ApiResponse<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderDetail(orderId))
                .build();
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("@permissionChecker.has('order:admin')")
    public ApiResponse<Void> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        orderService.updateOrderStatus(orderId, request);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.order.status-updated"))
                .build();
    }

    // ── WEBHOOK STUBS ─────────────────────────────────────────────────────

    /**
     * TODO: Webhook nhận sự kiện vận chuyển từ GHN.
     * Implement sau khi tích hợp GHN Shipping API.
     */
    @PostMapping("/webhooks/ghn")
    public ApiResponse<Void> ghnWebhook(@RequestBody Object payload) {
        // TODO: verify GHN signature, parse payload, update deliveryStatus + orderStatus
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.order.ghn-webhook"))
                .build();
    }
}
