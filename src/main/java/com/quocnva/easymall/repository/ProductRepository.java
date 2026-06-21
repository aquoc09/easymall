package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
