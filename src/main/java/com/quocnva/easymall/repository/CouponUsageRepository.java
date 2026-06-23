package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.CouponEntity;
import com.quocnva.easymall.entity.CouponUsageEntity;
import com.quocnva.easymall.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponUsageRepository extends JpaRepository<CouponUsageEntity, Long> {

    /** Đếm tổng lượt đã dùng toàn sàn để check max_usage */
    long countByCoupon(CouponEntity coupon);

    /** Đếm lượt dùng của 1 user cụ thể để check user_usage_limit */
    long countByCouponAndUser(CouponEntity coupon, UserEntity user);

    /**
     * Rollback coupon khi hủy đơn.
     * Dùng deleteByOrderId (không tìm theo used_at = NULL) để đảm bảo đúng
     * dù COD hay Online đều xóa sạch lịch sử dùng mã cho orderId này.
     */
    @Modifying
    @Query("DELETE FROM CouponUsageEntity cu WHERE cu.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    boolean existsByUserAndCouponAndOrderId(UserEntity user, CouponEntity coupon, Long orderId);
}
