package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ProductSimilarityEntity;
import com.quocnva.easymall.entity.ProductSimilarityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductSimilarityRepository extends JpaRepository<ProductSimilarityEntity, ProductSimilarityId> {

    @Query("SELECT ps FROM ProductSimilarityEntity ps JOIN FETCH ps.similarProduct WHERE ps.id.productId = :productId ORDER BY ps.score DESC LIMIT :limit")
    List<ProductSimilarityEntity> findTopSimilarProducts(@Param("productId") Long productId, @Param("limit") int limit);
}
