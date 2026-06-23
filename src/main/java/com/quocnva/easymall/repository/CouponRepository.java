package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.CouponEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    Optional<CouponEntity> findByCode(String code);

    boolean existsByCode(String code);

    Page<CouponEntity> findAllByOrderByCouponIdDesc(Pageable pageable);

    Page<CouponEntity> findByIsActiveTrueOrderByCouponIdDesc(Pageable pageable);
}
