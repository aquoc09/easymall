package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    boolean existsBySkuCode(String skuCode);

    List<ProductVariantEntity> findAllByProductProductId(Long productId);
}
