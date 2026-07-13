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
public class ProductAssociationId implements Serializable {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "related_product_id", nullable = false)
    private Long relatedProductId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductAssociationId that = (ProductAssociationId) o;
        return Objects.equals(productId, that.productId) &&
               Objects.equals(relatedProductId, that.relatedProductId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, relatedProductId);
    }
}
