package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ProductAssociationEntity;
import com.quocnva.easymall.entity.ProductAssociationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductAssociationRepository extends JpaRepository<ProductAssociationEntity, ProductAssociationId> {

    @Query("SELECT pa FROM ProductAssociationEntity pa JOIN FETCH pa.relatedProduct WHERE pa.id.productId = :productId ORDER BY pa.confidence DESC, pa.lift DESC LIMIT :limit")
    List<ProductAssociationEntity> findTopBoughtTogetherProducts(@Param("productId") Long productId, @Param("limit") int limit);
}
