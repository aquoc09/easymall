package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.OrderDetailEntity;
import com.quocnva.easymall.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, Long> {

    List<OrderDetailEntity> findByOrder(OrderEntity order);

    Optional<OrderDetailEntity> findByOrderAndVariant_VariantId(OrderEntity order, Long variantId);

    boolean existsByVariant_VariantId(Long variantId);
}
