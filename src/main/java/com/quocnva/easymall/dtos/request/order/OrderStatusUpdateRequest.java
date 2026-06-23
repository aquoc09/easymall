package com.quocnva.easymall.dtos.request.order;

import com.quocnva.easymall.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateRequest {

    @NotNull(message = "New status must not be null")
    private OrderStatus newStatus;
}
