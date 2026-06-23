package com.quocnva.easymall.dtos.response.order;

import com.quocnva.easymall.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CheckoutResponse {

    private Long orderId;
    private OrderStatus orderStatus;
    private BigDecimal finalPaymentMoney;

    /**
     * URL thanh toán online (VNPAY/MoMo).
     * COD → null.
     * Online → null hiện tại.
     * TODO: integrate payment gateway để sinh paymentUrl thực tế.
     */
    private String paymentUrl;
}
