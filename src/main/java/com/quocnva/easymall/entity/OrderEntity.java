package com.quocnva.easymall.entity;

import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.enums.PaymentMethod;
import com.quocnva.easymall.enums.ShippingMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Builder.Default
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate = LocalDate.now();

    @Column(name = "shipping_date")
    private LocalDate shippingDate;

    /** 0: COD, 1: VNPAY, 2: MOMO */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    /** 0: Standard, 1: Express */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "shipping_method", nullable = false)
    private ShippingMethod shippingMethod;

    /** Mã đơn giao hàng từ GHN / Ahamove */
    @Column(name = "delivery_order_id", length = 50)
    private String deliveryOrderId;

    @Builder.Default
    @Column(name = "delivery_status", length = 50)
    private String deliveryStatus = "PENDING";

    @Builder.Default
    @Column(name = "tracking_number", nullable = false, length = 100)
    private String trackingNumber = "";

    @Column(name = "note", length = 200)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private AddressEntity address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 50)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_session_id")
    private DeviceSessionEntity deviceSession;

    // ── Tầng 1: Tiền hàng ────────────────────────────────────────────

    @Column(name = "total_product_money", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalProductMoney;

    @Builder.Default
    @Column(name = "shop_discount_amount", precision = 15, scale = 2)
    private BigDecimal shopDiscountAmount = BigDecimal.ZERO;

    // ── Tầng 2: Vận chuyển ───────────────────────────────────────────

    @Builder.Default
    @Column(name = "original_shipping_fee", precision = 15, scale = 2)
    private BigDecimal originalShippingFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "shipping_discount_amount", precision = 15, scale = 2)
    private BigDecimal shippingDiscountAmount = BigDecimal.ZERO;

    // ── Tầng 3: Thanh toán ───────────────────────────────────────────

    @Builder.Default
    @Column(name = "payment_discount_amount", precision = 15, scale = 2)
    private BigDecimal paymentDiscountAmount = BigDecimal.ZERO;

    // ── Kết quả cuối ─────────────────────────────────────────────────

    @Column(name = "final_payment_money", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalPaymentMoney;

    // ── Relationships ─────────────────────────────────────────────────

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetailEntity> orderDetails = new ArrayList<>();
}
