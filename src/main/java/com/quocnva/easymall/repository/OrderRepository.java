package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /** User xem danh sách đơn của mình */
    Page<OrderEntity> findByUserOrderByOrderIdDesc(UserEntity user, Pageable pageable);

    /** Admin xem tất cả đơn */
    Page<OrderEntity> findAllByOrderByOrderIdDesc(Pageable pageable);

    /** User xem chi tiết 1 đơn — đảm bảo ownership */
    Optional<OrderEntity> findByOrderIdAndUser(Long orderId, UserEntity user);

    /** Admin filter theo trạng thái */
    Page<OrderEntity> findByOrderStatusOrderByOrderIdDesc(OrderStatus orderStatus, Pageable pageable);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.user.userId = :userId AND o.orderStatus = :status AND o.orderDate >= :since")
    long countOrdersByUserAndStatusSince(@Param("userId") Long userId, @Param("status") OrderStatus status,
            @Param("since") OffsetDateTime since);

    /** GHN Webhook — tìm order bằng tracking_number (= GHN order_code) */
    Optional<OrderEntity> findByTrackingNumber(String trackingNumber);

    /** Đếm số đơn hàng theo danh sách trạng thái (Dành cho Dashboard) */
    long countByOrderStatusIn(List<OrderStatus> statuses);

    /** Tính tổng doanh thu theo danh sách trạng thái (Dành cho Dashboard) */
    @Query("SELECT COALESCE(SUM(o.finalPaymentMoney), 0) FROM OrderEntity o WHERE o.orderStatus IN :statuses")
    BigDecimal calculateTotalRevenueByStatusIn(@Param("statuses") List<OrderStatus> statuses);
}
