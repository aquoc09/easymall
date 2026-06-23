package com.quocnva.easymall.dtos.response.order;

import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class OrderSummaryResponse {

    private Long orderId;
    private LocalDate orderDate;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal finalPaymentMoney;
    private Integer itemCount;
}
