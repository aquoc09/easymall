package com.quocnva.easymall.dtos.response.dashboard;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatResponse {
    OrderStatsDto orderStats;
    long totalProducts;
    long totalCategories;
    long totalSliders;
    long totalPermissions;
    long totalRoles;
    long totalReviews;
    long totalRiskAccounts;
    long totalUsers;
    long totalCoupons;
}
