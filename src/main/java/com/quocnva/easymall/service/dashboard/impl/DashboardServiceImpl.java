package com.quocnva.easymall.service.dashboard.impl;

import com.quocnva.easymall.dtos.response.dashboard.DashboardStatResponse;
import com.quocnva.easymall.dtos.response.dashboard.OrderStatsDto;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.repository.*;
import com.quocnva.easymall.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SliderRepository sliderRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final ReviewRepository reviewRepository;
    private final UserStatsRepository userStatsRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatResponse getDashboardOverview() {
        List<OrderStatus> pendingStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.PENDING_REVIEW, 
                OrderStatus.PENDING_PAYMENT, OrderStatus.AWAITING_SHIPMENT, 
                OrderStatus.SHIPPING
        );
        List<OrderStatus> successStatuses = List.of(
                OrderStatus.DELIVERED, OrderStatus.COMPLETED
        );
        List<OrderStatus> failedStatuses = List.of(
                OrderStatus.CANCELLED, OrderStatus.RETURNED, OrderStatus.REFUND_FAILED
        );

        long pendingOrders = orderRepository.countByOrderStatusIn(pendingStatuses);
        long successOrders = orderRepository.countByOrderStatusIn(successStatuses);
        long failedOrders = orderRepository.countByOrderStatusIn(failedStatuses);
        
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenueByStatusIn(successStatuses);

        OrderStatsDto orderStats = OrderStatsDto.builder()
                .totalPendingOrders(pendingOrders)
                .totalSuccessOrders(successOrders)
                .totalFailedOrders(failedOrders)
                .totalRevenue(totalRevenue)
                .build();

        return DashboardStatResponse.builder()
                .orderStats(orderStats)
                .totalProducts(productRepository.count())
                .totalCategories(categoryRepository.count())
                .totalSliders(sliderRepository.count())
                .totalPermissions(permissionRepository.count())
                .totalRoles(roleRepository.count())
                .totalReviews(reviewRepository.count())
                .totalRiskAccounts(userStatsRepository.countByIsRestrictedTrue())
                .totalUsers(userRepository.count())
                .totalCoupons(couponRepository.count())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        List<OrderStatus> successStatuses = List.of(
                OrderStatus.DELIVERED, OrderStatus.COMPLETED
        );
        return orderRepository.calculateTotalRevenueByStatusIn(successStatuses);
    }
}
