package com.quocnva.easymall.service.product;

/**
 * ProductStatsService — quản lý cập nhật các stats denormalized trên bảng products.
 * Các stats này được application cập nhật trực tiếp (không qua Hibernate entity),
 * sử dụng @Modifying + Native Query để tránh dirty checking overhead.
 */
public interface ProductStatsService {

    /**
     * Tăng view_count thêm 1. Gọi mỗi khi client fetch product detail.
     */
    void incrementViewCount(Long productId);

    /**
     * Cập nhật rating_avg và rating_count sau khi một review được APPROVED.
     * Tính lại trực tiếp từ bảng reviews bằng AVG() / COUNT().
     */
    void updateRatingStats(Long productId);

    /**
     * Tăng sold_count theo quantity. Gọi khi order chuyển sang trạng thái COMPLETED.
     */
    void updateSoldCount(Long productId, int quantity);
}
