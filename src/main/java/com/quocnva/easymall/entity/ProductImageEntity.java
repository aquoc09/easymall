package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "is_thumbnail", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isThumbnail;

    @Column(name = "display_order", columnDefinition = "INT DEFAULT 0")
    private Integer displayOrder;

    @PrePersist
    protected void onCreate() {
        if (isThumbnail == null) isThumbnail = false;
        if (displayOrder == null) displayOrder = 0;
    }
}
