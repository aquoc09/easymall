package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /** User xem danh sách đơn của mình */
    Page<OrderEntity> findByUserOrderByOrderIdDesc(UserEntity user, Pageable pageable);

    /** Admin xem tất cả đơn */
    Page<OrderEntity> findAllByOrderByOrderIdDesc(Pageable pageable);

    /** User xem chi tiết 1 đơn — đảm bảo ownership */
    Optional<OrderEntity> findByOrderIdAndUser(Long orderId, UserEntity user);

    /** Admin filter theo trạng thái */
    Page<OrderEntity> findByOrderStatusOrderByOrderIdDesc(OrderStatus orderStatus, Pageable pageable);
}
