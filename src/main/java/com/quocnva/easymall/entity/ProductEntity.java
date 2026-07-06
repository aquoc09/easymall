package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_slug", nullable = false, unique = true, length = 100)
    private String productSlug;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "product_description", columnDefinition = "text")
    private String productDescription;

    @Column(name = "in_popular")
    private Boolean inPopular;

    @Column(name = "in_stock")
    private Boolean inStock;

    /**
     * target_gender: 0=Nữ, 1=Nam, 2=Unisex.
     * Dùng SMALLINT, không dùng enum tránh migration phức tạp.
     */
    @Column(name = "target_gender")
    private Short targetGender;

    @Column(name = "max_order_quantity")
    private Integer maxOrderQuantity;

    /**
     * options_config: JSONB — cấu hình các option của sản phẩm (ví dụ: màu, size).
     * Lưu dưới dạng String JSON, convert thủ công qua service/mapper.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "options_config", columnDefinition = "jsonb")
    private String optionsConfig;

    /**
     * product_tags: JSONB — danh sách tag. Ví dụ: ["vintage", "oversize"].
     * DEFAULT '[]'::jsonb phía DB.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "product_tags", columnDefinition = "jsonb")
    private String productTags;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "length_m", precision = 5, scale = 2)
    private BigDecimal lengthM;

    @Column(name = "width_m", precision = 5, scale = 2)
    private BigDecimal widthM;

    @Column(name = "height_m", precision = 5, scale = 2)
    private BigDecimal heightM;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Denormalized stats — managed by DB trigger (min/max) and application/cron (others).
     * insertable=false, updatable=false: Hibernate không ghi vào các cột này.
     */
    @Column(name = "min_price", insertable = false, updatable = false, precision = 15, scale = 2)
    private java.math.BigDecimal minPrice;

    @Column(name = "max_price", insertable = false, updatable = false, precision = 15, scale = 2)
    private java.math.BigDecimal maxPrice;

    @Column(name = "view_count", insertable = false, updatable = false)
    private Integer viewCount;

    @Column(name = "sold_count", insertable = false, updatable = false)
    private Integer soldCount;

    @Column(name = "rating_avg", insertable = false, updatable = false, precision = 3, scale = 2)
    private java.math.BigDecimal ratingAvg;

    @Column(name = "rating_count", insertable = false, updatable = false)
    private Integer ratingCount;

    @Column(name = "popularity_score", insertable = false, updatable = false, precision = 10, scale = 4)
    private java.math.BigDecimal popularityScore;

    /**
     * search_vector: TSVECTOR — được DB trigger tự động cập nhật, không set từ Java.
     * insertable=false, updatable=false để Hibernate không ghi vào cột này.
     */
    @Column(name = "search_vector", insertable = false, updatable = false, columnDefinition = "tsvector")
    private String searchVector;

    // ── Relationships ──────────────────────────────────────────────────

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductVariantEntity> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImageEntity> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (inPopular == null)       inPopular = false;
        if (inStock == null)         inStock = true;
        if (targetGender == null)    targetGender = (short) 2;
        if (maxOrderQuantity == null) maxOrderQuantity = 0;
    }
}
