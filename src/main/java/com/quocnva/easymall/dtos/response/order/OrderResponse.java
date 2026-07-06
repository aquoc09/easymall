package com.quocnva.easymall.dtos.response.order;

import com.quocnva.easymall.dtos.response.address.AddressResponse;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.enums.PaymentMethod;
import com.quocnva.easymall.enums.ShippingMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderResponse {

    private Long orderId;
    private OffsetDateTime orderDate;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private ShippingMethod shippingMethod;
    private String trackingNumber;
    private String note;

    // ── Tài chính ──────────────────────────────────────────────────
    private BigDecimal totalProductMoney;
    private BigDecimal originalShippingFee;
    private BigDecimal shippingDiscountAmount;
    private BigDecimal paymentDiscountAmount;
    private BigDecimal finalPaymentMoney;

    // ── Embedded ───────────────────────────────────────────────────────
    private AddressResponse address;
    private List<OrderDetailResponse> items;
}
