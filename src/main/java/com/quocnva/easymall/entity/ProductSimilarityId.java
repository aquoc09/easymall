package com.quocnva.easymall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSimilarityId implements Serializable {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "similar_product_id", nullable = false)
    private Long similarProductId;

    @Column(name = "similarity_type", nullable = false, length = 50)
    private String similarityType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductSimilarityId that = (ProductSimilarityId) o;
        return Objects.equals(productId, that.productId) &&
               Objects.equals(similarProductId, that.similarProductId) &&
               Objects.equals(similarityType, that.similarityType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, similarProductId, similarityType);
    }
}
