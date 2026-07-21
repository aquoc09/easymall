package com.quocnva.easymall.service.dashboard;

import com.quocnva.easymall.dtos.response.dashboard.DashboardStatResponse;

public interface DashboardService {
    DashboardStatResponse getDashboardOverview();
    java.math.BigDecimal getTotalRevenue();
}
