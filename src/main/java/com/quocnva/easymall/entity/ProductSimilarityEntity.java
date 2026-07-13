package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_similarities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSimilarityEntity {

    @EmbeddedId
    private ProductSimilarityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("similarProductId")
    @JoinColumn(name = "similar_product_id", insertable = false, updatable = false)
    private ProductEntity similarProduct;

    @Column(name = "score", nullable = false)
    private Double score;
}
