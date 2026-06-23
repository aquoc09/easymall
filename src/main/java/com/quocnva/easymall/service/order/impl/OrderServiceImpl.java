package com.quocnva.easymall.service.order.impl;

import com.quocnva.easymall.dtos.request.order.CheckoutRequest;
import com.quocnva.easymall.dtos.request.order.OrderCancelRequest;
import com.quocnva.easymall.dtos.request.order.OrderStatusUpdateRequest;
import com.quocnva.easymall.dtos.response.order.CheckoutResponse;
import com.quocnva.easymall.dtos.response.order.OrderResponse;
import com.quocnva.easymall.dtos.response.order.OrderSummaryResponse;
import com.quocnva.easymall.entity.*;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.enums.PaymentMethod;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.mapper.OrderMapper;
import com.quocnva.easymall.repository.*;
import com.quocnva.easymall.service.coupon.CouponService;
import com.quocnva.easymall.service.coupon.impl.CouponServiceImpl;
import com.quocnva.easymall.service.device.DeviceSessionService;
import com.quocnva.easymall.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final CouponServiceImpl couponServiceImpl;
    private final DeviceSessionService deviceSessionService;
    private final OrderMapper orderMapper;

    // ── USER ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResponse checkout(CheckoutRequest request, String userEmail, HttpServletRequest httpRequest) {

        // Step 1: Resolve User
        UserEntity user = getUserByEmail(userEmail);

        // Step 2: Resolve & validate Address (phải thuộc user)
        AddressEntity address = addressRepository
                .findByAddressIdAndUser_UserId(request.getAddressId(), user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        // Step 3: Extract & persist DeviceSession
        DeviceSessionEntity deviceSession = deviceSessionService.getOrCreate(httpRequest, user);

        // Step 4: Load & validate CartItems thuộc user
        List<CartItemEntity> cartItems = cartItemRepository
                .findAllByCartItemIdIn(request.getCartItemIds());

        if (cartItems.isEmpty() || cartItems.size() != request.getCartItemIds().size()) {
            throw new AppException(ErrorCode.CART_ITEMS_NOT_FOUND);
        }
        // Đảm bảo tất cả cart items thuộc cart của user
        cartItems.forEach(item -> {
            if (!item.getCart().getUser().getUserId().equals(user.getUserId())) {
                throw new AppException(ErrorCode.CART_ITEMS_NOT_FOUND);
            }
        });

        // Step 5: Re-validate từng variant (stock, active, maxOrderQty)
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = item.getVariant();

            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                throw new AppException(ErrorCode.PRODUCT_BANNED);
            }

            // stockQuantity == -1 → infinite stock, bỏ qua check
            if (variant.getStockQuantity() != -1) {
                int available = variant.getStockQuantity() - variant.getLockedStock();
                if (available < item.getQuantity()) {
                    throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
                }
            }

            int maxQty = variant.getProduct().getMaxOrderQuantity();
            if (maxQty > 0 && item.getQuantity() > maxQty) {
                throw new AppException(ErrorCode.MAX_ORDER_QUANTITY_EXCEEDED);
            }
        }

        // Step 6: Validate coupon (nếu có)
        CouponEntity coupon = null;
        BigDecimal totalProductMoney = cartItems.stream()
                .map(item -> item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shopDiscountAmount     = BigDecimal.ZERO;
        BigDecimal shippingDiscountAmount = BigDecimal.ZERO;
        BigDecimal paymentDiscountAmount  = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponService.validateForCheckout(request.getCouponCode(), totalProductMoney, userEmail);
            BigDecimal discount = couponServiceImpl.calculateDiscount(coupon, totalProductMoney);
            switch (coupon.getCouponType()) {
                case SHOP_VOUCHER    -> shopDiscountAmount     = discount;
                case FREE_SHIPPING   -> shippingDiscountAmount = discount;
                case PAYMENT_VOUCHER -> paymentDiscountAmount  = discount;
            }
        }

        // Step 7: Tính toán tài chính
        BigDecimal originalShippingFee = BigDecimal.ZERO;
        // TODO: gọi GHN fee API để lấy phí ship thực tế

        BigDecimal finalPaymentMoney = totalProductMoney
                .subtract(shopDiscountAmount)
                .add(originalShippingFee)
                .subtract(shippingDiscountAmount)
                .subtract(paymentDiscountAmount);

        // Step 8: Lock inventory (Pessimistic Write đã được JPA đảm bảo trong @Transactional)
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = productVariantRepository
                    .findById(item.getVariant().getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
            variant.setLockedStock(variant.getLockedStock() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        // Step 9: Tạo Order
        OrderEntity order = OrderEntity.builder()
                .user(user)
                .address(address)
                .deviceSession(deviceSession)
                .paymentMethod(request.getPaymentMethod())
                .shippingMethod(request.getShippingMethod())
                .note(request.getNote())
                .totalProductMoney(totalProductMoney)
                .shopDiscountAmount(shopDiscountAmount)
                .originalShippingFee(originalShippingFee)
                .shippingDiscountAmount(shippingDiscountAmount)
                .paymentDiscountAmount(paymentDiscountAmount)
                .finalPaymentMoney(finalPaymentMoney)
                .orderStatus(OrderStatus.PENDING)
                .build();

        List<OrderDetailEntity> details = new ArrayList<>();
        for (CartItemEntity item : cartItems) {
            BigDecimal itemTotal = item.getVariant().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            details.add(OrderDetailEntity.builder()
                    .order(order)
                    .variant(item.getVariant())
                    .numOfProduct(item.getQuantity())
                    .orderDetailPrice(item.getVariant().getPrice())
                    .totalMoney(itemTotal)
                    .build());
        }
        order.setOrderDetails(details);
        OrderEntity savedOrder = orderRepository.save(order);

        // Step 10: Commit CouponUsage
        if (coupon != null) {
            CouponUsageEntity usage = CouponUsageEntity.builder()
                    .user(user)
                    .coupon(coupon)
                    .orderId(savedOrder.getOrderId())
                    .build();
            couponUsageRepository.save(usage);
        }

        // Step 11: Xóa CartItems đã checkout
        cartItemRepository.deleteAllByCartItemIdIn(request.getCartItemIds());

        // Step 12: Payment routing
        OrderStatus finalStatus;
        String paymentUrl = null;

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            finalStatus = OrderStatus.AWAITING_SHIPMENT;
        } else {
            finalStatus = OrderStatus.PENDING_PAYMENT;
            // TODO: gọi VNPAY/MoMo API để sinh paymentUrl thực tế
        }
        savedOrder.setOrderStatus(finalStatus);
        orderRepository.save(savedOrder);

        return CheckoutResponse.builder()
                .orderId(savedOrder.getOrderId())
                .orderStatus(finalStatus)
                .finalPaymentMoney(finalPaymentMoney)
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(String userEmail, Pageable pageable) {
        UserEntity user = getUserByEmail(userEmail);
        return orderRepository.findByUserOrderByOrderIdDesc(user, pageable)
                .map(orderMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderDetail(Long orderId, String userEmail) {
        UserEntity user = getUserByEmail(userEmail);
        OrderEntity order = orderRepository.findByOrderIdAndUser(orderId, user)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelMyOrder(Long orderId, OrderCancelRequest request, String userEmail) {
        UserEntity user = getUserByEmail(userEmail);
        OrderEntity order = orderRepository.findByOrderIdAndUser(orderId, user)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Chỉ cho hủy khi đơn đang PENDING
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.CANCELLATION_NOT_ALLOWED);
        }

        // Giải phóng locked_stock
        for (OrderDetailEntity detail : order.getOrderDetails()) {
            ProductVariantEntity variant = productVariantRepository
                    .findById(detail.getVariant().getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
            int newLocked = Math.max(0, variant.getLockedStock() - detail.getNumOfProduct());
            variant.setLockedStock(newLocked);
            productVariantRepository.save(variant);
        }

        // Rollback CouponUsage
        couponUsageRepository.deleteByOrderId(orderId);

        // TODO: nếu paymentStatus == PAID → gọi cổng thanh toán để hoàn tiền (REFUND API)

        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByOrderIdDesc(pageable)
                .map(orderMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        order.setOrderStatus(request.getNewStatus());
        orderRepository.save(order);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
