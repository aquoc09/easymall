package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ReviewEntity;
import com.quocnva.easymall.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    /** Lấy reviews APPROVED của sản phẩm (public) */
    Page<ReviewEntity> findByProduct_ProductIdAndReviewStatus(Long productId, ReviewStatus status, Pageable pageable);

    /** Lấy reviews của chính user */
    Page<ReviewEntity> findByUser_UserId(Long userId, Pageable pageable);

    /** Kiểm tra user đã review sản phẩm trong đơn hàng này chưa */
    boolean existsByUser_UserIdAndProduct_ProductIdAndOrder_OrderId(Long userId, Long productId, Long orderId);

    /** Đếm review theo rating để tính summary */
    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.product.productId = :productId AND r.reviewStatus = 'APPROVED' GROUP BY r.rating")
    java.util.List<Object[]> countByRatingForProduct(@Param("productId") Long productId);

    /** Tính average rating */
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.product.productId = :productId AND r.reviewStatus = 'APPROVED'")
    Optional<Double> findAverageRatingByProductId(@Param("productId") Long productId);

    /** Đếm tổng APPROVED reviews */
    long countByProduct_ProductIdAndReviewStatus(Long productId, ReviewStatus status);
}
