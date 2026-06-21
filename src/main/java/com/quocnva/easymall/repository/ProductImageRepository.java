package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findAllByProductProductIdOrderByDisplayOrderAsc(Long productId);
}
