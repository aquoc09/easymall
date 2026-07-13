package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_associations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAssociationEntity {

    @EmbeddedId
    private ProductAssociationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("relatedProductId")
    @JoinColumn(name = "related_product_id", insertable = false, updatable = false)
    private ProductEntity relatedProduct;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "lift", nullable = false)
    private Double lift;
}
