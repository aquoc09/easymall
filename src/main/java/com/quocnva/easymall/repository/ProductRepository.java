package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>,
        JpaSpecificationExecutor<ProductEntity> {

    boolean existsByProductSlug(String productSlug);

    Optional<ProductEntity> findByProductSlug(String productSlug);

    /**
     * Dùng để check xem category có đang được sử dụng bởi product nào không
     * (phục vụ CategoryServiceImpl.checkCategoryHasProducts).
     */
    boolean existsByCategoryId(Long categoryId);

    // ── Stats Queries (Native — không qua Hibernate dirty checking) ────────
    
    // Fallback cho Recommendation Cold Start
    List<ProductEntity> findTop10ByInStockTrueOrderBySoldCountDesc();

    @Modifying
    @Query(value = "UPDATE products SET view_count = view_count + 1 WHERE product_id = :productId", nativeQuery = true)
    void incrementViewCount(@Param("productId") Long productId);

    /**
     * Tính lại rating_avg và rating_count trực tiếp từ bảng reviews.
     * Chỉ tính reviews đã được APPROVED (status = 'APPROVED').
     */
    @Modifying
    @Query(value = """
        UPDATE products p
        SET rating_avg   = COALESCE((
                SELECT ROUND(AVG(r.rating)::NUMERIC, 2)
                FROM reviews r
                WHERE r.product_id = :productId AND r.status = 'APPROVED'
            ), 0.00),
            rating_count = COALESCE((
                SELECT COUNT(*) FROM reviews r
                WHERE r.product_id = :productId AND r.status = 'APPROVED'
            ), 0)
        WHERE p.product_id = :productId
        """, nativeQuery = true)
    void recalculateRatingStats(@Param("productId") Long productId);

    @Modifying
    @Query(value = "UPDATE products SET sold_count = sold_count + :qty WHERE product_id = :productId", nativeQuery = true)
    void increaseSoldCount(@Param("productId") Long productId, @Param("qty") int qty);
}

