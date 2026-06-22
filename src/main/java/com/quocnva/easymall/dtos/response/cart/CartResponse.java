package com.quocnva.easymall.dtos.response.cart;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long cartId;

    /**
     * Tổng tiền chỉ tính từ các item hợp lệ (available = true).
     */
    private BigDecimal totalAmount;

    private List<CartItemResponse> items;
}
