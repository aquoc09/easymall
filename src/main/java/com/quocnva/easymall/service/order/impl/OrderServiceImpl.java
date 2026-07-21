package com.quocnva.easymall.service.order.impl;

import com.quocnva.easymall.dtos.request.order.CheckoutRequest;
import com.quocnva.easymall.dtos.request.order.OrderCancelRequest;
import com.quocnva.easymall.dtos.request.order.OrderStatusUpdateRequest;
import com.quocnva.easymall.dtos.response.ghn.GhnCreateOrderResponse;
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
import com.quocnva.easymall.service.ghn.GhnOrderService;
import com.quocnva.easymall.service.order.OrderService;
import com.quocnva.easymall.event.OrderCreatedEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final GhnOrderService ghnOrderService;
    private final com.quocnva.easymall.service.ghn.GhnShippingService ghnShippingService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.quocnva.easymall.service.payment.VnPayService vnPayService;

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

        // Step 3: Extract & persist DeviceSession (để RiskEngine xử lý sau)
        deviceSessionService.getOrCreate(httpRequest, user);

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

        BigDecimal totalProductMoney = cartItems.stream()
                .map(item -> item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 6: Tính toán tài chính - GHN Shipping Fee
        int totalWeightGram = cartItems.stream()
                .mapToInt(item -> {
                    BigDecimal weightKg = item.getVariant().getProduct().getWeightKg();
                    if (weightKg == null)
                        return 500;
                    return weightKg.multiply(BigDecimal.valueOf(1000))
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .intValue();
                })
                .sum();

        com.quocnva.easymall.dtos.request.ghn.ShippingFeeRequest feeRequest = new com.quocnva.easymall.dtos.request.ghn.ShippingFeeRequest();
        feeRequest.setToDistrictId(address.getDistrictId());
        feeRequest.setToWardCode(address.getWardCode());
        feeRequest.setWeightGram(totalWeightGram);

        // Mặc định sử dụng phương thức vận chuyển Standard (2) do frontend không cho
        // phép chọn
        feeRequest.setServiceId(53320);

        com.quocnva.easymall.dtos.response.ghn.GhnShippingFeeResponse feeResponse = ghnShippingService
                .calculateFee(feeRequest);
        BigDecimal originalShippingFee = BigDecimal.valueOf(feeResponse.getTotal());

        // Step 7: Validate coupon (nếu có)
        List<CouponEntity> appliedCoupons = new ArrayList<>();
        BigDecimal shopDiscountAmount = BigDecimal.ZERO;
        BigDecimal shippingDiscountAmount = BigDecimal.ZERO;
        BigDecimal paymentDiscountAmount = BigDecimal.ZERO;

        if (request.getCouponCodes() != null && !request.getCouponCodes().isEmpty()) {
            for (String code : request.getCouponCodes()) {
                CouponEntity coupon = couponService.validateForCheckout(code, totalProductMoney, userEmail,
                        request.getPaymentMethod());
                BigDecimal discount = couponServiceImpl.calculateDiscount(coupon, totalProductMoney,
                        originalShippingFee);
                switch (coupon.getCouponType()) {
                    case SHOP_VOUCHER -> shopDiscountAmount = shopDiscountAmount.add(discount);
                    case FREE_SHIPPING -> shippingDiscountAmount = shippingDiscountAmount.add(discount);
                    case PAYMENT_VOUCHER -> paymentDiscountAmount = paymentDiscountAmount.add(discount);
                }
                appliedCoupons.add(coupon);
            }
        }

        BigDecimal finalPaymentMoney = totalProductMoney
                .subtract(shopDiscountAmount)
                .add(originalShippingFee)
                .subtract(shippingDiscountAmount)
                .subtract(paymentDiscountAmount);

        // Step 8: Lock inventory (Pessimistic Write đã được JPA đảm bảo trong
        // @Transactional)
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = productVariantRepository
                    .findById(item.getVariant().getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
            variant.setLockedStock(variant.getLockedStock() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        String trackingNumber = com.quocnva.easymall.config.VnPayProperties.getRandomNumber(8);

        OrderEntity order = OrderEntity.builder()
                .user(user)
                .address(address)
                .trackingNumber(trackingNumber)
                .paymentMethod(request.getPaymentMethod())
                .shippingMethod(request.getShippingMethod())
                .note(request.getNote())
                .totalProductMoney(totalProductMoney)
                .originalShippingFee(originalShippingFee)
                .shopDiscountAmount(shopDiscountAmount)
                .shippingDiscountAmount(shippingDiscountAmount)
                .paymentDiscountAmount(paymentDiscountAmount)
                .finalPaymentMoney(finalPaymentMoney)
                .orderStatus(OrderStatus.PENDING)
                .build();

        List<OrderDetailEntity> details = new ArrayList<>();
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = item.getVariant();
            BigDecimal itemTotal = variant.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            details.add(OrderDetailEntity.builder()
                    .order(order)
                    .variant(variant)
                    .productName(variant.getProduct().getProductName())
                    .variantAttributes(variant.getVariantAttributes())
                    .skuCode(variant.getSkuCode())
                    .variantImage(variant.getVariantImage())
                    .numOfProduct(item.getQuantity())
                    .orderDetailPrice(variant.getPrice())
                    .totalMoney(itemTotal)
                    .build());
        }
        order.setOrderDetails(details);
        OrderEntity savedOrder = orderRepository.save(order);

        // Step 10: Commit CouponUsage
        if (!appliedCoupons.isEmpty()) {
            for (CouponEntity appliedCoupon : appliedCoupons) {
                CouponUsageEntity usage = CouponUsageEntity.builder()
                        .user(user)
                        .coupon(appliedCoupon)
                        .orderId(savedOrder.getOrderId())
                        .build();
                couponUsageRepository.save(usage);
            }
        }

        // Step 11: Xóa CartItems đã checkout
        cartItemRepository.deleteAllByCartItemIdIn(request.getCartItemIds());

        // Step 12: Phát sự kiện OrderCreatedEvent cho Risk Engine (Phase 8) chạy ngầm
        eventPublisher.publishEvent(new OrderCreatedEvent(this, savedOrder.getOrderId(), user.getUserId()));

        // Step 13: Payment routing
        OrderStatus finalStatus;
        String paymentUrl = null;

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            finalStatus = OrderStatus.AWAITING_SHIPMENT;
        } else if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            finalStatus = OrderStatus.PENDING_PAYMENT;
            String ip = com.quocnva.easymall.config.VnPayProperties.getIpAddress(httpRequest);
            com.quocnva.easymall.dtos.request.payment.VnPayPaymentRequest paymentRequest = com.quocnva.easymall.dtos.request.payment.VnPayPaymentRequest
                    .builder()
                    .trackingNumber(order.getTrackingNumber())
                    .ipAddress(ip)
                    .amount(order.getFinalPaymentMoney().longValue())
                    .language("vn")
                    .build();
            try {
                paymentUrl = vnPayService.createPaymentUrl(paymentRequest);
            } catch (Exception e) {
                throw new AppException(ErrorCode.PAYMENT_URL_CREATION_FAILED);
            }
        } else {
            finalStatus = OrderStatus.PENDING_PAYMENT;
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

        // TODO: nếu paymentStatus == PAID → gọi cổng thanh toán để hoàn tiền (REFUND
        // API)

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

        OrderStatus newStatus = request.getNewStatus();
        order.setOrderStatus(newStatus);

        // Phase 7: Tự động tạo đơn GHN khi Admin chuyển sang SHIPPING
        if (newStatus == OrderStatus.SHIPPING) {
            GhnCreateOrderResponse ghnOrder = ghnOrderService.createOrder(order);
            order.setDeliveryOrderId(ghnOrder.getOrderCode());
            order.setTrackingNumber(ghnOrder.getOrderCode());
            order.setDeliveryStatus("READY_TO_PICK");
        }

        orderRepository.save(order);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
