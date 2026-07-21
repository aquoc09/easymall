package com.quocnva.easymall.dtos.response.dashboard;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatsDto {
    long totalPendingOrders;
    long totalSuccessOrders;
    long totalFailedOrders;
    BigDecimal totalRevenue;
}
