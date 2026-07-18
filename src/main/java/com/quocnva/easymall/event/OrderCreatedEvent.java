package com.quocnva.easymall.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderCreatedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long userId;

    public OrderCreatedEvent(Object source, Long orderId, Long userId) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
    }
}
