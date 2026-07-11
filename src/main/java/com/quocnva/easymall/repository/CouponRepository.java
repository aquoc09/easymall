package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.CouponEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    Optional<CouponEntity> findByCode(String code);

    boolean existsByCode(String code);

    Page<CouponEntity> findAllByOrderByCouponIdDesc(Pageable pageable);

    Page<CouponEntity> findByIsActiveTrueOrderByCouponIdDesc(Pageable pageable);

    @Query("SELECT c FROM CouponEntity c WHERE c.isActive = true AND c.startDate <= :now AND c.endDate >= :now ORDER BY c.couponId DESC")
    List<CouponEntity> findAvailableCoupons(@Param("now") OffsetDateTime now);
}
