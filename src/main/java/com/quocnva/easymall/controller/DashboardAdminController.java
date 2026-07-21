package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.dashboard.DashboardStatResponse;
import com.quocnva.easymall.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardAdminController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("@permissionChecker.has('dashboard:view')")
    public ApiResponse<DashboardStatResponse> getDashboardOverview() {
        DashboardStatResponse stats = dashboardService.getDashboardOverview();
        return ApiResponse.<DashboardStatResponse>builder()
                .result(stats)
                .build();
    }

    @GetMapping("/revenue")
    @PreAuthorize("@permissionChecker.has('dashboard:view')")
    public ApiResponse<BigDecimal> getTotalRevenue() {
        BigDecimal totalRevenue = dashboardService.getTotalRevenue();
        return ApiResponse.<BigDecimal>builder()
                .result(totalRevenue)
                .build();
    }
}
